package com.github.libpq4s

import scala.concurrent.Promise
import scala.util.Try

import com.github.libpq4s.api._
import com.github.libpq4s.library._

class Connection private[libpq4s](connectDbFn: () => IPGconn)(implicit libpq: ILibPQ) extends AbstractConnection() {

  def this(connInfo: String)(implicit libpq: ILibPQ)  {
    this(() => libpq.PQconnectdb(connInfo))
  }

  def this(params: Seq[(String,String)])(implicit libpq: ILibPQ)  {
    this(() => libpq.PQconnectdbParams(params, expandDbName = false))
  }

  lazy val copyManager = new CopyManager(this)

  def open(): Connection = {
    conn = connectDbFn()

    /* Check to see that the backend connection was successfully made */
    if (this.checkStatus() != ConnStatusType.CONNECTION_OK) {
      throw new Exception(s"connection to database failed: ${this.lastErrorMessage()}")
    }

    this
  }

}

abstract class AbstractConnection private[libpq4s]()(protected[libpq4s] implicit val libpq: ILibPQ) extends Logging with AutoCloseable {

  protected var conn: IPGconn = _
  @inline def underlying: IPGconn = conn
  def isClosed(): Boolean = conn == null

  /** Closes the database connection */
  def close(): Unit = {
    require(!isClosed(), "already closed") // TODO: should we fail?
    libpq.PQfinish(conn)
    conn = null
  }

  lazy val connectionOptions: Seq[ConnInfoOption] = {
    ConnectionFactory.connOptionPtr2connOptions(libpq.PQconninfo(conn)) // PQconninfoFree() called internally
  }

  lazy val databaseName: String = libpq.PQdb(conn)
  lazy val user: String = libpq.PQuser(conn)
  lazy val password: String = libpq.PQpass(conn)
  lazy val host: String = libpq.PQhost(conn)
  lazy val port: String = libpq.PQport(conn)
  lazy val commandLineOptions: String = libpq.PQoptions(conn)

  def cancelCurrentCommand(): Try[Unit] = {
    val cancelPtr = libpq.PQgetCancel(conn)
    val cancelResult = libpq.PQcancel(cancelPtr)
    libpq.PQfreeCancel(cancelPtr)
    cancelResult
  }

  def reset(checkStatus: Boolean = true): Unit = {
    libpq.PQreset(conn)
    if (checkStatus) {
      if (libpq.PQstatus(conn) == ConnStatusType.CONNECTION_OK) {
        throw new Exception("connection reset failed because " + libpq.PQerrorMessage(conn))
      }
    }
  }

  def checkStatus(): IConnStatusType = libpq.PQstatus(conn)

  def checkTransactionStatus(): ITransactionStatusType = libpq.PQtransactionStatus(conn)

  def retrieveParamValue(paramName: String): Option[String] = {
    Option(libpq.PQparameterStatus(conn, paramName))
  }

  def getProtocolVersion(): Int = libpq.PQprotocolVersion(conn)

  def getServerVersion(): Int = libpq.PQserverVersion(conn)

  def lastErrorMessage(): Option[String] = Option(libpq.PQerrorMessage(conn))

  def retrieveBackendPID(): Int = libpq.PQbackendPID(conn)

  def checkPasswordNeeded(): Boolean = libpq.PQconnectionNeedsPassword(conn)

  def checkPasswordUsed(): Boolean = libpq.PQconnectionUsedPassword(conn)

  def getClientEncoding(): String = {
    val encodingId = libpq.PQclientEncoding(conn)
    libpq.pg_encoding_to_char(encodingId)
  }

  def setClientEncoding(encoding: String): Boolean = {
    libpq.PQsetClientEncoding(conn, encoding)
  }

  def setErrorVerbosity(verbosity: IPGVerbosity): IPGVerbosity = {
    libpq.PQsetErrorVerbosity(conn, verbosity)
  }

  def setErrorContextVisibility(showContext: IPGContextVisibility): IPGContextVisibility = {
    libpq.PQsetErrorContextVisibility(conn, showContext)
  }

  /* ----- Query execution methods ----- */

  /*def executeAndProcess(query: String)(processFn: QueryResult => Unit): Int = {
    val execResult = libpq.PQexec(conn, query)
    _checkPQExecResult(execResult)

    val queryResult = new QueryResult(execResult)
    val affectedRowsCount = queryResult.getAffectedRowsCount()

    processFn(queryResult)

    queryResult.close()

    affectedRowsCount
  }*/

  def executeCommand(query: String, paramValues: String*): Int = {
    val execResult = if (paramValues.isEmpty) libpq.PQexec(conn, query)
    else libpq.PQexecParamsText(conn, query, paramValues, null)

    PGresultUtils.checkExecutionStatus(underlying, execResult, ExecStatusType.PGRES_COMMAND_OK, clearOnMismatch = true)

    val queryResult = new QueryResult(execResult)
    val affectedRowsCount = queryResult.getAffectedRowsCount()

    queryResult.close()

    affectedRowsCount
  }

  def executeAndProcess(query: String, paramValues: String*)(processFn: QueryResult => Unit): Unit = {
    val execResult = if (paramValues.isEmpty) libpq.PQexec(conn, query)
    else libpq.PQexecParamsText(conn, query, paramValues, null)

    // TODO: should we check something (PGRES_FATAL_ERROR, PGRES_BAD_RESPONSE, PGRES_NONFATAL_ERROR)???

    val queryResult = new QueryResult(execResult)

    processFn(queryResult)

    queryResult.close()
  }

  def executeAndIterate(query: String, paramValues: String*)(onEachRow: IRowData[String] => Unit): Unit = {
    val execResult = if (paramValues.isEmpty) libpq.PQexec(conn, query)
    else libpq.PQexecParamsText(conn, query, paramValues, null)

    PGresultUtils.checkExecutionStatus(underlying, execResult, ExecStatusType.PGRES_TUPLES_OK, clearOnMismatch = true)

    val pgResultConsumedPromise = Promise[Unit]
    val iter = new IterableQueryResult(conn, execResult, singleRowMode = true, pgResultConsumedPromise)

    iter.foreachRow(onEachRow)
  }

  /*def executeBinaryData(query: String, paramValues: Array[Byte]*): QueryResult = {
    val res = libpq.PQexecParamsBinary(conn, query, paramValues, null)
    new QueryResult(res)
  }*/

  def prepareTextStatement(query: String): TextStatement = {

    val prepareResult = libpq.PQprepare(
      conn,
      stmtName = query,
      query = query,
      paramTypes = null
    )

    // FIXME: can we assume to get a PGRES_COMMAND_OK status???
    PGresultUtils.checkExecutionStatus(underlying, prepareResult, ExecStatusType.PGRES_COMMAND_OK, clearOnMismatch = true)

    new TextStatement(conn,prepareResult, stmtName = query)
  }

}
