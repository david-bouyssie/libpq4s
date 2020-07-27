package com.github.libpq4s

import scala.util._
import com.github.libpq4s.api._
import com.github.libpq4s.library._

// TODO: implement this tuto https://gist.github.com/ictlyh/12fe787ec265b33fd7e4b0bd08bc27cb
class CopyManager private[libpq4s](connection: Connection)(private[libpq4s] implicit val libpq: ILibPQ) {

  def copyIn(sql: String): CopyIn = {
    val execResult = libpq.PQexec(connection.underlying, sql)
    PGresultUtils.checkExecutionStatus(connection.underlying, execResult, ExecStatusType.PGRES_COPY_IN)

    new CopyIn(connection.underlying, execResult)
  }

  def copyOut(sql: String): CopyOut = {
    val execResult = libpq.PQexec(connection.underlying, sql)
    PGresultUtils.checkExecutionStatus(connection.underlying, execResult, ExecStatusType.PGRES_COPY_OUT)

    new CopyOut(connection.underlying, execResult)
  }

}

trait CopyOperation {

  private[libpq4s] implicit def libpq: ILibPQ
  protected var res: IPGresult

  def close(): Unit = {
    if (res != null) {
      libpq.PQclear(res)
      res = null
    }
  }

  def countColumns(): Int = libpq.PQnfields(res)

  def isBinaryFormat(): Boolean = libpq.PQbinaryTuples(res)

  def findColumnFormat(colNum: Int): Int = libpq.PQfformat(res, colNum)

  def isActive: Boolean = res != null

  // TODO: implement me, or use connection.cancelCurrentCommand()???
  //def cancelCopy(): Unit

  // TODO: can we know this?
  //def getHandledRowCount: Long
}

class CopyIn private[libpq4s](conn: IPGconn, protected var res: IPGresult)(private[libpq4s] implicit val libpq: ILibPQ) extends CopyOperation  {

  def writeToCopy(data: Array[Byte], offset: Int = 0, size: Int = 0): Unit = {
    require(res != null, "res is null")
    _checkPutResult(libpq.PQputCopyData(conn, data, offset, size))
  }

  // def flushCopy(): Unit

  def cancelCopy(): Unit = this._endCopy( Some("cancel COPY requested"))

  def endCopy(): Unit = this._endCopy(None)

  // TODO: find a way to return getHandledRowCount
  private def _endCopy(errorMsg: Option[String]): Unit = {
    require(res != null, "res is null")

    _checkPutResult(libpq.PQputCopyEnd(conn, errorMsg))

    // After successfully calling PQputCopyEnd, call PQgetResult to obtain the final result status of the COPY command.
    val finalRes = libpq.PQgetResult(conn)
    try {
      PGresultUtils.checkExecutionStatus(conn, finalRes, ExecStatusType.PGRES_COMMAND_OK)
    } finally {
      libpq.PQclear(finalRes)
      this.close()
    }

  }

  private def _checkPutResult(putResult: Try[Boolean] ): Unit = {
    putResult match {
      case Success(true) => ()
      case Success(false) => throw new Exception("unexpected CopyIn state, it should only happen in nonblocking mode")
      case Failure(t) => throw t
    }
  }

}

object CopyInUtils {

  /**
   * Replace empty strings and None entries by the '\N' character and convert the List[Any] to a byte array.
   * Note: by default '\N' means NULL value for the postgres COPY function
   */
  def rowToTextBytes(row: Seq[Any], escape: Boolean = true): Array[Byte] = {
    if (row == null) return Array.empty[Byte]

    val stringBuilder = new scala.collection.mutable.StringBuilder()
    appendRowToStringBuilder(row, stringBuilder, escape)

    stringBuilder.result.getBytes("UTF-8")
  }

  /**
   * Replace empty strings and None entries by the '\N' character and convert the List[Any] to a byte array.
   * Note: by default '\N' means NULL value for the postgres COPY function
   */
  def appendRowToStringBuilder(row: Seq[Any], stringBuilder: collection.mutable.StringBuilder, escape: Boolean = true): Unit = {
    if (row == null) return

    val nItems = row.length
    val lastIndex = nItems - 1

    var i = 0
    while (i < nItems) {
      row(i) match {
        case opt: Option[Any] => {
          opt match {
            case None => stringBuilder ++= """\N"""
            case Some(value) => _stringify(value, stringBuilder, escape)
          }
        }
        case value: Any => _stringify(value, stringBuilder, escape)
      }


      if (i == lastIndex) stringBuilder ++= "\n" else stringBuilder ++= "\t"

      i += 1
    }
  }

  @inline private def _stringify(value: Any, stringBuilder: collection.mutable.StringBuilder, escape: Boolean): Unit = {
    value match {
      case s: String => if (escape) _escapeStringForPgCopy(s, stringBuilder) else stringBuilder ++= s
      case a: Any => stringBuilder ++= a.toString
    }
  }

  @inline private def _escapeStringForPgCopy(str: String, stringBuilder: collection.mutable.StringBuilder): Unit = {
    val strLen = str.length

    var i = 0
    while (i < strLen) {
      str.charAt(i) match {
        case '\\' => stringBuilder ++= """\\"""
        case '\r' => ()
        case '\n' => stringBuilder ++= """\n"""
        case '\t' => stringBuilder ++= """\t"""
        case char: Char  => stringBuilder += char
      }

      i += 1
    }

    ()
  }
}

class CopyOut private[libpq4s](conn: IPGconn, protected var res: IPGresult)(private[libpq4s] implicit val libpq: ILibPQ) extends CopyOperation {

  def readFromCopy(): Option[Array[Byte]] = {
    require(res != null, "res is null")

    libpq.PQgetCopyData(conn, async = false) match {
      case Left(row) => Some(row)
      case Right(rc) => {
        // -1 indicates that the COPY is done => we return None
        if (rc == -1) {
          this.close()
          None
        }
        else {
          this.close()

          val errorMsg = s" readFromCopy() failed with error '${libpq.PQerrorMessage(conn)}'"
          throw new Exception(errorMsg)
        }
      }
    }
  }

}


