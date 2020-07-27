package com.github.libpq4s

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._

import com.github.libpq4s.api._
import com.github.libpq4s.library._


// TODO: implement this tuto https://gist.github.com/ictlyh/12fe787ec265b33fd7e4b0bd08bc27cb
class AsyncCopyManager private[libpq4s](connection: AsyncConnection)(private[libpq4s] implicit val libpq: ILibPQ) {

  def copyIn(sql: String): Future[AsyncCopyIn] = {
    connection.sendQueryAndIterate(sql).map { resultIter =>

      val pgConn = connection.underlying
      val pgResult = resultIter.queryResult.underlying
      PGresultUtils.checkExecutionStatus(pgConn, pgResult, ExecStatusType.PGRES_COPY_IN)

      new AsyncCopyIn(pgConn, pgResult)
    }
  }

  def copyOut(sql: String): Future[AsyncCopyOut] = {
    connection.sendQueryAndIterate(sql).map { resultIter =>

      val pgConn = connection.underlying
      val pgResult = resultIter.queryResult.underlying
      PGresultUtils.checkExecutionStatus(pgConn, pgResult, ExecStatusType.PGRES_COPY_OUT)

      new AsyncCopyOut(pgConn, pgResult)
    }
  }

}

trait AsyncCopyOperation extends CopyOperation with ConnectionPolling with Logging

class AsyncCopyIn private[libpq4s](conn: IPGconn, protected var res: IPGresult)(private[libpq4s] implicit val libpq: ILibPQ) extends AsyncCopyOperation  {

  def writeToCopy(data: Array[Byte], offset: Int = 0, size: Int = 0): Future[Unit] = {
    require(res != null, "res is null")

    require(libpq.PQisnonblocking(conn))

    libpq.PQputCopyData(conn, data, offset, size) match {
      case Failure(t) => return Future.failed(t)
      case Success(true) => return Future.successful( () )
      case Success(false) => () // continue
    }

    // If the result is zero, the data was not queued because of full buffers, thus wait for write-ready and try again.
    val connPollHandle = this.initPolling(conn)

    val readable = false
    val writable = true

    val writePromise = Promise[Unit]

    var i = 0
    connPollHandle.start(in = readable, out = writable) { (socketStatus, socketReadable, socketWritable) =>
      i += 1

      // If promise already completed => try to leave the event loop
      if (writePromise.isCompleted) {
        this.stopPolling(connPollHandle)
      } else {

        if (socketStatus < 0) {
          logger.error(s"Bad socket status: $socketStatus")
          // TODO: also check that socket is readable & writable?
        } else if (socketWritable) {
          logger.debug(
            s"After $i iterations, socket ready with status $socketStatus and writable = $socketWritable"
          )

          libpq.PQputCopyData(conn, data, offset, size) match {
            case Failure(t) => writePromise.failure(t)
            case Success(true) => {
              logger.debug(s"After $i iterations, data COPY was written")

              writePromise.success( () )
            }
            case Success(false) => () // continue
          }

        }

      } // ends else writePromise.isCompleted
    }

    writePromise.future.recover { case t =>
      this.close()
      throw t
    }
  }

  def cancelCopy(): Future[Unit] = this._endCopy( Some("cancel COPY requested"))

  def endCopy(): Future[Unit] = this._endCopy(None)

  // TODO: find a way to return getHandledRowCount
  private def _endCopy(errorMsg: Option[String]): Future[Unit] = {

    libpq.PQputCopyEnd(conn, errorMsg) match {
      case Failure(t) => return Future.failed(t)
      case _  => ()
    }

    val connPollHandle = this.initPolling(conn)

    val readable = false
    val writable = true

    val endCopyPromise = Promise[Unit]

    var i = 0
    connPollHandle.start(in = readable, out = writable) { (socketStatus, socketReadable, socketWritable) =>
      i += 1

      // If promise already completed => try to leave the event loop
      if (endCopyPromise.isCompleted) {
        this.stopPolling(connPollHandle)
      } else {

        // In nonblocking mode, to be certain that the data has been sent,
        // you should next wait for write-ready and call PQflush, repeating until it returns zero
        if (socketStatus < 0) {
          logger.error(s"Bad socket status: $socketStatus")
          // TODO: also check that socket is readable & writable?
        } else if (socketWritable) {

          logger.debug(
            s"After $i iterations, socket ready with status $socketStatus and writable = $socketWritable"
          )

          val flushingStatus = libpq.PQflush(conn)

          if (flushingStatus == -1) {
            val errMsg = libpq.PQerrorMessage(conn)
            endCopyPromise.failure(
              new Exception(s"endCopy() failed: $errMsg")
            )
          } else if (flushingStatus == 1) {
            logger.trace("Need to flush again, was unable to send all the data in the send queue yet")
          } else if (flushingStatus == 0) {

            logger.debug(s"After $i iterations, endCopy() finished")

            endCopyPromise.success( () )
          }

        }
      } // ends else endCopyPromise.isCompleted
    }

    val result = endCopyPromise.future.map { _ =>
      // After successfully calling PQputCopyEnd, call PQgetResult to obtain the final result status of the COPY command.
      // One can wait for this result to be available in the usual way.
      // Then return to normal operation.
      val finalRes = libpq.PQgetResult(conn)
      try {
        PGresultUtils.checkExecutionStatus(conn, res, ExecStatusType.PGRES_COMMAND_OK)
      }
      finally {
        libpq.PQclear(finalRes)
      }

      ()
    }
    result.onComplete { case _ =>
      this.close()
    }
    result
  }

  /*private def _checkPutResult(putResult: Try[Boolean] ): Boolean = {
    putResult match {
      case Success(true) => true
      case Success(false) => false // throw new Exception("can't put data in the queue because buffers may be full")
      case Failure(t) => throw t
    }
  }*/

}

class AsyncCopyOut private[libpq4s](conn: IPGconn, protected var res: IPGresult)(private[libpq4s] implicit val libpq: ILibPQ) extends AsyncCopyOperation {

  def readFromCopy(): Future[Option[Array[Byte]]] = {

    // When async is true (not zero), PQgetCopyData will not block waiting for input
    // it will return zero if the COPY is still in progress but no complete row is available.
    // In this case wait for read-ready and then call PQconsumeInput before calling PQgetCopyData again.
    val connPollHandle = this.initPolling(conn)

    val readable = true
    val writable = false

    val readFromCopyPromise = Promise[Option[Array[Byte]]]

    var needToCallGetCopyData = true
    var isInputConsumed = false

    var i = 0
    connPollHandle.start(in = readable, out = writable) { (socketStatus, socketReadable, socketWritable) =>
      i += 1

      // If promise already completed => try to leave the event loop
      if (readFromCopyPromise.isCompleted) {
        this.stopPolling(connPollHandle)
      } else {
        if (needToCallGetCopyData) {
          libpq.PQgetCopyData(conn, async = true) match {
            case Left(row) => readFromCopyPromise.success(Some(row))
            case Right(rc) => {
              // -1 indicates that the COPY is done => we return None
              if (rc == -1) readFromCopyPromise.success(None)
              else if (rc == 0) None
              else {
                this.close()
                val errorMsg = s"readFromCopy() failed with error '${libpq.PQerrorMessage(conn)}'"
                readFromCopyPromise.failure(new Exception(errorMsg))
              }
            }
          }

          needToCallGetCopyData = false
        }

        if (!readFromCopyPromise.isCompleted) {

          if (socketStatus < 0) {
            logger.error(s"Bad socket status: $socketStatus")
            // TODO: also check that socket is readable & writable?
          } else if (socketReadable) {

            logger.debug(
              s"After $i iterations, socket ready with status $socketStatus and readable = $socketReadable"
            )

            isInputConsumed = libpq.PQconsumeInput(conn)
            if (!isInputConsumed) {
              val errMsg = libpq.PQerrorMessage(conn)
              readFromCopyPromise.failure(
                new Exception(s"readFromCopy() failed: $errMsg")
              )
            }
          }

          if (isInputConsumed) {
            if (libpq.PQisBusy(conn)) {
              logger.trace("server is busy")
            } else {
              needToCallGetCopyData = true
            }
          }
        }
      } // ends else readFromCopyPromise.isCompleted

    } // ends connPollHandle.start

    val result = readFromCopyPromise.future.map { rowOpt =>

      logger.debug(s"After $i iterations, data COPY was read")

      if (rowOpt.isEmpty) {

        // After PQgetCopyData returns -1, call PQgetResult to obtain the final result status of the COPY command.
        // One can wait for this result to be available in the usual way.
        // Then return to normal operation.
        val finalRes = libpq.PQgetResult(conn)
        try {
          PGresultUtils.checkExecutionStatus(conn, finalRes, ExecStatusType.PGRES_COMMAND_OK)
        } finally {
          libpq.PQclear(finalRes)
        }
      }
      rowOpt
    }

    result.onComplete { _ =>
      this.close()
    }

    result

  }
  
}


