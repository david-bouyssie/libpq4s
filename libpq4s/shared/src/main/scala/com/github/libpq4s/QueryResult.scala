package com.github.libpq4s

import com.github.libpq4s.api._
import com.github.libpq4s.library._

// TODO: value class???
class QueryResult private[libpq4s](private var res: IPGresult)(private[libpq4s] implicit val libpq: ILibPQ) extends AutoCloseable {

  def underlying: IPGresult = res

  def isClosed(): Boolean = res == null

  def close(): Unit = {
    if (!isClosed) {
      libpq.PQclear(res)
      res = null
    }
  }

  def checkStatus(): IExecStatusType = libpq.PQresultStatus(res)
  def getErrorMessage(): String = libpq.PQresultErrorMessage(res)
  def retrieveErrorField(fieldCode: IPGErrorFieldIdentifier): Option[String] = Option(libpq.PQresultErrorField(res, fieldCode))
  def countRows(): Int = libpq.PQntuples(res)
  def countColumns(): Int = libpq.PQnfields(res)

  def findColumnName(colNum: Int): Option[String] = Option(libpq.PQfname(res, colNum))
  def findColumnNumber(colName: String): Option[Int] = {
    val colNum = libpq.PQfnumber(res, colName)
    if (colNum == -1) None else Some(colNum)
  }
  def findTableOid(colNum: Int): IOidBox = libpq.PQftable(res, colNum)
  def findColumnNumberInTable(colNum: Int): Int = libpq.PQftablecol(res, colNum)
  def findColumnFormat(colNum: Int): Int = libpq.PQfformat(res, colNum)
  def findColumnDataType(colNum: Int): IOidBox = libpq.PQftype(res, colNum)
  def findColumnSize(colNum: Int): Int = libpq.PQfsize(res, colNum)
  def findColumnTypeModifier(colNum: Int): Int = libpq.PQfmod(res, colNum)

  def getCmdStatus(): String = libpq.PQcmdStatus(res)
  def getInsertRowOid(): IOidBox = libpq.PQoidValue(res)
  def getAffectedRowsCount(): Int = libpq.PQcmdTuples(res)

  def getFieldValue(rowNum: Int, colNum: Int): String = libpq.PQgetvalue(res, rowNum, colNum)
  def getFieldLength(rowNum: Int, colNum: Int): Int = libpq.PQgetlength(res, rowNum, colNum)
  def checkFieldIsNull(res: IPGresult, rowNum: Int, colNum: Int): Boolean = libpq.PQgetisnull(res, rowNum, colNum)

  // --- PQdescribePrepared related methods --- //
  // TODO: add boolean to check if statement was prepared???
  def countPreparedParams(res: IPGresult): Int = libpq.PQnparams(res)
  def retrievePreparedParamType(res: IPGresult, paramNum: Int): IOidBox = libpq.PQparamtype(res, paramNum)
}