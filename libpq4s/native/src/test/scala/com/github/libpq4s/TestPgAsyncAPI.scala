package com.github.libpq4s

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import utest._

import com.github.libpq4s.api.ConnStatusType._

import com.github.libpq4s.library._

object TestPgAsyncAPI extends TestSuite with Logging {

  // TODO: put this in a custom TestFramework
  Logging.configureLogger(minLogLevel = LogLevel.TRACE)

  private implicit val pq: ILibPQ = LibPQ

  private val resourceName = "postgres"
  private val _connInfo = s"host=127.0.0.1 port=5555 user=$resourceName password=$resourceName dbname=$resourceName"

  val tests = Tests {
    'createAndPopulateCarsTable - createAndPopulateCarsTable()
    'testAsyncQuery - testAsyncQuery()
  }

  private def _tryWithConnection(autoClose: Boolean = true)(fn: Connection => Unit): Unit = {

    /* Make a connection to the database */
    val conn = new Connection(_connInfo).open()

    try {

      /* Check to see that the backend connection was successfully made */
      if (conn.checkStatus() != CONNECTION_OK) {
        throw new Exception(s"connection to database failed: ${conn.lastErrorMessage()}")
      }

      fn(conn)

    } finally {
      /* close the connection to the database and cleanup */
      if (autoClose) conn.close()
    }
  }

  /*private def _checkPQExecResult(res: IPGresult, conn: IPGconn, clearResult: Boolean = false, command: String = "Last Postgres"): Unit = {
    if (clearResult) PQclear(res)

    PGresultUtils.checkExecutionStatus(conn.underlying, res.underlying, PGRES_COMMAND_OK, clearOnMismatch = true)
  }*/

  private def createAndPopulateCarsTable(): Unit = {

    _tryWithConnection(autoClose = true) { conn =>

      conn.executeCommand("DROP TABLE IF EXISTS Cars")

      conn.executeCommand("CREATE TABLE Cars(Id INTEGER PRIMARY KEY, Name VARCHAR(20), Price INT)")

      for (i <- 0 to 10) {
        val inc = i * 10

        conn.executeCommand(s"INSERT INTO Cars VALUES(${1 + inc},'Audi',52642)")

        conn.executeCommand(s"INSERT INTO Cars VALUES(${2 + inc},'Mercedes',57127)")

        conn.executeCommand(s"INSERT INTO Cars VALUES(${3 + inc},'Skoda',9000)")

        conn.executeCommand(s"INSERT INTO Cars VALUES(${4 + inc},'Volvo',29000)")

        conn.executeCommand(s"INSERT INTO Cars VALUES(${5 + inc},'Bentley',350000)")

        conn.executeCommand(s"INSERT INTO Cars VALUES(${7 + inc},'Citroen',21000)")

        conn.executeCommand(s"INSERT INTO Cars VALUES(${8 + inc},'Hummer',41400)")

        conn.executeCommand(s"INSERT INTO Cars VALUES(${9 + inc},'Volkswagen',21600)")
      }

    }
  }

  /*
  class PgAsyncQuery private[libpq4s](
    connWrapper: PgAsyncConnection,
    conn: IPGconn,
    query: String,
    singleRowMode: Boolean
  ) extends PgConnPolling {

    private var _connPollHandle: Poll = _

    val result: Future[PgAsyncResult] = _send()

    private def _send(): Future[PgAsyncResult] = {
      require(connWrapper.isConnected(), "not connected")

      logger.debug(s"sending query: $query")

      val queryStartTime = now()

      val queryPromise = Promise[PgAsyncResult]

      /* Check again that the backend connection was successfully made */
      val connStatus = PQstatus(conn)
      logger.debug(s"PQstatus equals $connStatus")

      /* To prevent PQsendQuery from blocking, we make a call to PQsetnonblocking with a nonzero argument */
      // TODO: put this in open() method???
      val nonBlockingValue = 1
      val isNonblocking = PQsetnonblocking(conn, nonBlockingValue) == 0

      if (connStatus == CONNECTION_BAD || !isNonblocking) {
        val errMsg = PQerrorMessage(conn)
        connWrapper.close()

        queryPromise.failure(
          new Exception(s"Connection to database lost: $errMsg")
        )

        return queryPromise.future
      }

      // Double check that connection is effectively set to nonblocking mode
      assert(PQisnonblocking(conn) == 1)

      // Submit query to the server without waiting for the results
      // True is returned if the command was successfully dispatched and false if not
      // (in which case, use PQerrorMessage to get more information about the failure).
      if (!PQsendQuery(conn, query)) {
        val errMsg = PQerrorMessage(conn)
        queryPromise.failure(
          new Exception(s"Query failed: $errMsg")
        )

        return queryPromise.future
      }

      // If requested, enables single-row mode for the currently executing query
      if (singleRowMode) {
        PQsetSingleRowMode(conn)
      }

      /* Poll the socket underlying the database connection */
      _connPollHandle = this.initPolling(conn)

      var queryConsumed = false
      var queryFlushed = false
      var i = 0
      _connPollHandle.start(in = true, out = true) { (socketStatus, socketReadable, socketWritable) =>
        i += 1

        if (queryPromise.isCompleted) {
          this.stopPolling(_connPollHandle)
        } else {
          if (socketStatus < 0) {
            logger.debug(s"Bad socket status: $socketStatus")
            // TODO: also check that socket is readable & writable?
          } else if (!queryFlushed) { // query not yet flushed
            val flushingStatus = PQflush(conn)
            //logger.debug(s"flushingStatus: $flushingStatus")

            if (flushingStatus == -1) {
              val errMsg = PQerrorMessage(conn)
              queryPromise.failure(
                new Exception(s"Query failed: $errMsg")
              )
            } else if (flushingStatus == 0 || flushingStatus == 1) {
              //logger.debug(s"socketReadable: $socketReadable")
              if (queryConsumed && flushingStatus == 0) {
                logger.debug("query flushed")
                queryFlushed = true
              }
              else if (socketReadable) {
                logger.debug("calling PQconsumeInput")
                if (PQconsumeInput(conn) == 0) {
                  val errMsg = PQerrorMessage(conn)
                  queryPromise.failure(
                    new Exception(s"Query failed: $errMsg")
                  )
                } else {
                  queryConsumed = true
                }
              }
            } else { // should not happen
              queryPromise.failure(
                new Exception(s"Invalid flushing status value: $flushingStatus")
              )
            }

          } else { // query flushed
            if (PQisBusy(conn) == 0) {
              val curTime = now()
              val took = curTime - queryStartTime
              logger.debug(
                s"After $i iterations and $took, query result is ready with socket status $socketStatus, " +
                  s"readable = $socketReadable and writable = $socketWritable"
              )

              queryPromise.success(new PgAsyncResult(conn, singleRowMode))
            } else {
              logger.debug("server is busy")
            }
          }
        }

      } // ends polling loop

      queryPromise.future
    }
  }*/


  private def testAsyncQuery(): Future[Unit] = {

    def now(): Duration = System.currentTimeMillis().millis

    val asyncTestStart = now()

    val futures = (1 to 5).map { i =>

      val pgConn = new AsyncConnection(_connInfo)
      logger.debug(s"Opening async connection #$i")

      pgConn.open().recover { case t: Throwable =>
        logger.debug("Error during connection: " + t)
        pgConn
      } flatMap { conn =>
        assert(conn.isConnected())

        logger.debug("connected :)")

        conn.enableSingleRowMode()
        val f1 = conn.sendQueryAndIterate("SELECT * FROM Cars ORDER BY Name DESC LIMIT 6").flatMap { asyncRes =>
          logger.debug("received query1 results asynchronously :D")

          val nRows = asyncRes.foreachRow { row =>
            logger.debug(row.toString)
          }

          logger.debug(s"Received $nRows rows")

          conn.enableSingleRowMode()
          conn.sendQueryAndIterate("SELECT * FROM Cars ORDER BY Name, Price LIMIT 6").map { asyncRes =>
            if (asyncRes != null) {
              logger.debug("received query2 results asynchronously :D")

              val nRows = asyncRes.foreachRow { row =>
                logger.debug(row.toString)
              }

              logger.debug(s"Received $nRows rows")
            }

            ()
          }

        } recover { case t: Throwable =>
          logger.debug("Error during first future: " + t)
          ()
        }

        conn.disableSingleRowMode()
        val f2 = conn.sendQueryAndIterate("SELECT * FROM Cars ORDER BY price ASC LIMIT 6").map { asyncRes =>
          logger.debug("received query3 results asynchronously :D")

          val nRows = asyncRes.foreachRow { row =>
            logger.debug(row.toString)
          }

          logger.debug(s"Received $nRows rows")
        } recover { case t: Throwable =>
          logger.debug("Error during SELECT query: " + t)
        }

        Future.sequence( List(f1, f2) ).map { _ =>
          logger.debug(s"Closing async connection #$i")
          pgConn.close()
          ()
        }

      }
    }

    val finalFuture = Future.sequence(futures).map { _ =>

      val took = now() - asyncTestStart
      println(s"The connection(s) and the queries took $took")

      ()
    }

    //Await.ready(finalFuture, 60 seconds)

    finalFuture
  }

  /*def _printTuples(result: IPGresult) {
    val nrows = PQntuples(result)
    val nfields = PQnfields(result)
    println(s"number of rows returned = $nrows")
    println(s"number of fields returned = $nfields")

    for(r <- 0 until nrows; n <- 0 until nfields) {
      printf(" %s = %s(%d),\n",
        PQfname(result, n),
        PQgetvalue(result, r, n),
        PQgetlength(result, r, n)
      )
    }
  }*/

}
