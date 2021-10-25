package com.github.libpq4s

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.scalanative.loop.{Poll, RWResult}

import com.github.libpq4s.api._
import com.github.libpq4s.api.PostgresPollingStatusType._
import com.github.libpq4s.api.ConnStatusType._
import com.github.libpq4s.library._

// See: https://github.com/mauricio/postgresql-async/tree/master/db-async-common/src/main/scala/com/github/mauricio/async/db

/**
 * Manages a connection to the database server in a nonblocking manner.
 * See: https://www.postgresql.org/docs/12/libpq-connect.html
 */
class AsyncConnection private[libpq4s](connectStartFn: () => IPGconn)(implicit libpq: ILibPQ)
  extends AbstractConnection() with ConnectionPolling {

  private val pq = libpq
  import pq._

  /**
   * AsyncConnection connInfo constructor
   *
   * @param connInfo The passed string can be empty to use all default parameters,
   *                 or it can contain one or more parameter settings separated by whitespace,
   *                 or it can contain an URI.
   *                 See: https://www.postgresql.org/docs/12/libpq-connect.html#LIBPQ-CONNSTRING
   */
  def this(connInfo: String)(implicit libpq: ILibPQ)  {
    this(() => libpq.PQconnectStart(connInfo))
  }

  /** AsyncConnection params constructor */
  def this(params: Seq[(String,String)])(implicit libpq: ILibPQ)  {
    this(() => libpq.PQconnectStartParams(params, expandDbName = false))
  }

  protected def now(): Duration = System.currentTimeMillis().millis

  private var _connPollHandle: Poll = _
  private var _connPollingStatus: IPostgresPollingStatusType = _

  private var _isConnected = false
  def isConnected(): Boolean = _isConnected

  // FIXME: find a better way to configure this for each individual query => maybe an implicit query config?
  private var _singleRowModeEnabled = false
  def enableSingleRowMode(): AsyncConnection = {
    _singleRowModeEnabled = true
    this
  }
  def disableSingleRowMode(): AsyncConnection = {
    _singleRowModeEnabled = false
    this
  }

  lazy val copyManager = new AsyncCopyManager(this)

  /** Closes the database connection */
  override def close(): Unit = {
    super.close()
    _isConnected = false
  }

  /** Opens a new database connection using the parameters taken from the string connInfo. */
  def open(): Future[AsyncConnection] = {

    val connStartTime = now()

    val connectionPromise = Promise[AsyncConnection]()

    /* Start an asynchronous connection */
    // To begin a nonblocking connection request, call PQconnectStart or PQconnectStartParams.
    // If the result is null, then libpq has been unable to allocate a new PGconn structure.
    // Otherwise, a valid PGconn pointer is returned (though not yet representing a valid connection to the database).
    conn = connectStartFn()
    if (conn == null) {
      connectionPromise.failure( new Exception("unable to open a Postgres connection") )
    }

    /* Next call PQstatus(conn) to check that the backend connection was successfully made */
    // If the result is CONNECTION_BAD, the connection attempt has already failed,
    // typically because of invalid connection parameters.
    // At any time during connection, the status of the connection can be checked by calling PQstatus.
    // If this call returns CONNECTION_BAD, then the connection procedure has failed;
    // if the call returns CONNECTION_OK, then the connection is ready.
    // Both of these states are equally detectable from the return value of PQconnectPoll.
    // Other states might also occur during (and only during) an asynchronous connection procedure.
    // These indicate the current stage of the connection procedure and might be useful to provide feedback to the user for example.
    if (PQstatus(conn) == CONNECTION_BAD) { // Note: CONNECTION_STARTED expected
      //PQfinish(conn)
      connectionPromise.failure(new Exception(s"connection to database failed: ${PQerrorMessage(conn)}"))
    }

    /* Poll the socket underlying the database connection */
    _connPollHandle = this.initPolling(conn)

    val readable = true
    val writable = true

    /* Loop until PQconnectPoll returns PGRES_POLLING_FAILED or PGRES_POLLING_OK */
    // If PQconnectPoll(conn) last returned PGRES_POLLING_READING,
    // wait until the socket is ready to read (as indicated by select(), poll(), or similar system function).
    // Then call PQconnectPoll(conn) again.
    // Conversely, if PQconnectPoll(conn) last returned PGRES_POLLING_WRITING, wait until the socket is ready to write.
    // Then call PQconnectPoll(conn) again. On the first iteration,
    // i.e. if you have yet to call PQconnectPoll, behave as if it last returned PGRES_POLLING_WRITING.
    // Continue this loop until PQconnectPoll(conn) returns PGRES_POLLING_FAILED, indicating the connection procedure has failed,
    // or PGRES_POLLING_OK, indicating the connection has been successfully made.
    var errorThrown = false
    var i = 0
    _connPollHandle.start(in = readable, out = writable) { case rwResult =>
      i += 1

      // If promise already completed => try to leave the event loop
      if (connectionPromise.isCompleted) {
        this.stopPolling(_connPollHandle)
        /*if (errorThrown) {
          PQfinish(conn)
        }*/
      } else {
        // FIXME: should we acquire a new socket after each call to PQconnectPoll (see caution above)?
        _connPollingStatus = PQconnectPoll(conn)

        if (rwResult.result < 0) {
          logger.error(s"Bad socket status: ${rwResult.result}")
          // TODO: also check that socket is readable & writable?
        } else if (_connPollingStatus == PGRES_POLLING_OK) {
          val curTime = now()
          val took = curTime - connStartTime
          logger.debug(
            s"After $i iterations and $took, PQconnectPoll ready with socket status ${rwResult.result}, " +
              s"readable = ${rwResult.readable} and writable = ${rwResult.writable}"
          )

          /* To prevent PQsendQuery from blocking, we make a call to PQsetnonblocking with a true argument */
          val isNonblocking = PQsetnonblocking(conn, true)

          if (isNonblocking) {
            _isConnected = true
            connectionPromise.success(this)
          } else {
            connectionPromise.failure(new Exception("can't make the connection nonblocking"))
          }

        } else if (_connPollingStatus == PGRES_POLLING_FAILED) {
          errorThrown = true
          val errMsg = PQerrorMessage(conn)
          connectionPromise.failure(
            new Exception(s"connection polling failed: $errMsg")
          )
        }
      }
    }

    connectionPromise.future
  }

  // TODO: sendPrepareTextStatement + sendPreparedQuery?

  /**
   * Submits a command to the server asynchronously.
   *
   * If you have several commands you'd like to fire off as a batch,
   * you can put them in a single PQsendQuery (with semicolons between of course).
   *
   * A typical async application will have a main loop that uses select() or poll() to react to read and write events.
   * The socket will signal activity when there is back-end data to be processed,
   * but there are a couple of conditions that must be met for this to work.
   *
   * 1. There must be no data outstanding to be sent to the server.
   * We can ensure this by calling PQflush until it returns zero.
   * After sending any command or data on a nonblocking connection, call PQflush.
   * If it returns 1, wait for the socket to become read- or write-ready.
   * If it becomes write-ready, call PQflush again. If it becomes read-ready, call PQconsumeInput, then call PQflush again.
   * Repeat until PQflush returns 0.
   * It is necessary to check for read-ready and drain the input with PQconsumeInput,
   * because the server can block trying to send us data (e.g. NOTICE messages) and won't read our data until we read its.
   *
   * 2. There must be input available from the server, which in terms of select() means readable data on the file descriptor identified by PQsocket.
   * Once PQflush returns 0, wait for the socket to be read-ready and then read the response.
   *
   * 3. Data must be read from the connection by PQconsumeInput() before calling PQgetResult().
   * When the main loop detects input ready, it should call PQconsumeInput to read the input.
   * It can then call PQisBusy, followed by PQgetResult if PQisBusy returns false (0).
   *
   * 4. Keep reading PGresults until a NULL is returned.
   * It's important to note that it is not possible to issue another PQsendQuery until PGresults returned NULL.
   *
   * @param query
   */
  def sendQuery(query: String, paramValues: String*): Future[Unit] = {
    _sendQueryAndIterate(query, paramValues, this._singleRowModeEnabled).map { iterableResult =>
      iterableResult.close()
    }
  }

  private var _promiseQueue = new mutable.Queue[Promise[Unit]]()

  def sendQueryAndIterate(query: String, paramValues: String*): Future[IterableQueryResult] = {
    _sendQueryAndIterate(query, paramValues, this._singleRowModeEnabled)
  }

  private def _sendQueryAndIterate(query: String, paramValues: Seq[String], singleRowMode: Boolean): Future[IterableQueryResult] = {
    //logger.debug(s"_promiseQueue.isEmpty: ${_promiseQueue.isEmpty}")

    val pgResultConsumedPromise = Promise[Unit]

    if (_promiseQueue.isEmpty) {
      //println("********** PROMISE QUEUE IS EMPTY ******************")
      _promiseQueue += pgResultConsumedPromise
      this._sendQuery(query, paramValues, singleRowMode, pgResultConsumedPromise)/*.map { result =>
        logger.debug("ALOA")
        result
      }*/
    } else {
      // Enqueue pgResultConsumedPromise of current query
      _promiseQueue += pgResultConsumedPromise

      // Dequeue pgResultConsumedPromise of previous query
      val f = _promiseQueue.dequeue().future.flatMap { _ =>
        //logger.debug("GoTTTTTTTTTTT**********")

        val futureResult = this._sendQuery(query, paramValues, singleRowMode, pgResultConsumedPromise).map { result =>
          //logger.debug("ALOA")
          result
        }

        futureResult
      }

      f
    }

  }

  def _sendQuery(
    query: String,
    paramValues: Seq[String],
    singleRowMode: Boolean,
    pgResultConsumedPromise: Promise[Unit]
  ): Future[IterableQueryResult] = {
    require(_isConnected, "not connected")

    logger.debug(s"query: $query")
    //logger.debug("_isQueryInProgress: " + _isQueryInProgress)

    val queryStartTime = now()

    /* Check again that the backend connection was successfully made */
    val connStatus = PQstatus(conn)
    logger.debug(s"PQstatus equals $connStatus")

    val queryPromise = Promise[IterableQueryResult]

    //if (connStatus == CONNECTION_BAD || !isNonblocking) {
    if (connStatus == CONNECTION_BAD) {
      val errMsg = PQerrorMessage(conn)
      /*PQfinish(conn)
      _isConnected = false*/

      queryPromise.failure(
        new Exception(s"connection to database lost: $errMsg")
      )

      return queryPromise.future
    }

    // Double check that connection is effectively set to nonblocking mode
    assert(PQisnonblocking(conn))

    // Submit query to the server without waiting for the results
    // True is returned if the command was successfully dispatched and false if not
    // (in which case, use PQerrorMessage to get more information about the failure).
    val querySent = if (paramValues.isEmpty) PQsendQuery(conn, query)
    else PQsendQueryParamsText(conn, query, paramValues, null)

    if (!querySent) {
      val errMsg = PQerrorMessage(conn)
      logger.error(s"errMsg: $errMsg")
      queryPromise.failure(
        new Exception(s"query failed: $errMsg")
      )

      return queryPromise.future
    }

    //logger.debug("after PQsendQuery")

    // If requested, enables single-row mode for the currently executing query
    if (singleRowMode) {
      PQsetSingleRowMode(conn)
    }

    /* Poll the socket underlying the database connection */
    _connPollHandle = this.initPolling(conn)

    //logger.debug("after initPolling")

    var queryConsumed = false
    var queryFlushed = false
    var i = 0
    _connPollHandle.start(in = true, out = true) { rwResult =>
      i += 1

      if (queryPromise.isCompleted) {
        this.stopPolling(_connPollHandle)
      } else {
        if (rwResult.result < 0) {
          logger.error(s"Bad socket status: ${rwResult.result}")
          // TODO: also check that socket is readable & writable?
        } else if (!queryFlushed) { // query not yet flushed
          val flushingStatus = PQflush(conn)
          //logger.debug(s"flushingStatus: $flushingStatus")

          if (flushingStatus == -1) {
            val errMsg = PQerrorMessage(conn)
            queryPromise.failure(
              new Exception(s"query failed: $errMsg")
            )
          } else if (flushingStatus == 0 || flushingStatus == 1) {
            //logger.debug(s"socketReadable: $socketReadable")
            if (queryConsumed && flushingStatus == 0) {
              //logger.debug("query flushed")
              queryFlushed = true
            }
            else if (rwResult.readable) {
              //logger.debug("calling PQconsumeInput")
              if (!PQconsumeInput(conn)) {
                val errMsg = PQerrorMessage(conn)
                queryPromise.failure(
                  new Exception(s"query failed: $errMsg")
                )
              } else {
                queryConsumed = true
              }
            }
          } else { // should not happen
            queryPromise.failure(
              new Exception(s"invalid flushing status value: $flushingStatus")
            )
          }

        } else { // query flushed
          if (PQisBusy(conn)) {
            logger.trace("server is busy")
          } else {
            val curTime = now()
            val took = curTime - queryStartTime
            logger.debug(
              s"After $i iterations and $took, query result is ready with socket status ${rwResult.result}, "+
                s"readable = ${rwResult.readable} and writable = ${rwResult.writable}"
            )

            queryPromise.success(new IterableQueryResult(conn, PQgetResult(conn), singleRowMode, pgResultConsumedPromise))
          }
        }
      }

    } // ends polling loop

    queryPromise.future
  }

}
