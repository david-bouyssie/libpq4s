package com.github.libpq4s

import scala.concurrent.Promise

import com.github.libpq4s.api._
import com.github.libpq4s.library._

abstract class AbstractStatement[T] private[libpq4s](
  var conn: IPGconn,
  val res: IPGresult,
  val stmtName: String
)(private[libpq4s] implicit val libpq: ILibPQ) extends AutoCloseable {

  def isClosed(): Boolean = conn == null

  // TODO: better close() implem?
  def close(): Unit = {
    libpq.PQclear(res)
    conn = null
  }

  protected def checkPQExecResult(expectedStatus: IExecStatusType): Unit = {
    if (res == null || libpq.PQresultStatus(res) != expectedStatus) {
      val errMsg = libpq.PQerrorMessage(conn)
      libpq.PQclear(res)
      throw new Exception(s"query execution failed (status=${libpq.PQresultStatus(res)}) with error '$errMsg' ")
    }
  }

}

class TextStatement private[libpq4s](conn: IPGconn, res: IPGresult, stmtName: String)(implicit libpq: ILibPQ)
  extends AbstractStatement[String](conn, res, stmtName) {

  /*def execute(paramValues: Seq[String]): QueryResult = {
    val res = libpq.PQexecPreparedText(conn, stmtName, paramValues)
    new QueryResult(res)
  }*/

  def executeWith(paramValues: String*): Int = {
    val execResult = libpq.PQexecPreparedText(conn, stmtName, paramValues)
    checkPQExecResult(ExecStatusType.PGRES_COMMAND_OK)

    val queryResult = new QueryResult(execResult)
    val affectedRowsCount = queryResult.getAffectedRowsCount()

    queryResult.close()

    affectedRowsCount
  }

  def executeAndProcess(paramValues: String*)(processFn: QueryResult => Unit): Int = {
    val execResult = libpq.PQexecPreparedText(conn, stmtName, paramValues)

    val queryResult = new QueryResult(execResult)
    val affectedRowsCount = queryResult.getAffectedRowsCount()

    processFn(queryResult)

    queryResult.close()

    affectedRowsCount
  }

  def executeAndIterate(paramValues: String*)(onEachRow: IRowData[String] => Unit): Unit = {
    val execResult = libpq.PQexecPreparedText(conn, stmtName, paramValues)
    checkPQExecResult(ExecStatusType.PGRES_COMMAND_OK)

    val pgResultConsumedPromise = Promise[Unit]
    val iter = new IterableQueryResult(conn, execResult, singleRowMode = true, pgResultConsumedPromise)

    iter.foreachRow(onEachRow)
  }
}

class BinaryStatement private[libpq4s](conn: IPGconn, res: IPGresult, stmtName: String)(implicit libpq: ILibPQ)
  extends AbstractStatement[Array[Byte]](conn, res, stmtName) {

  def execute(paramValues: Seq[Array[Byte]]): QueryResult = {
    val res = libpq.PQexecPreparedBinary(conn, stmtName, paramValues)
    new QueryResult(res)
  }
}