package com.github.libpq4s

import com.github.libpq4s.api.ExecStatusType._
import com.github.libpq4s.api._
import com.github.libpq4s.library._

import scala.concurrent._

/**
 * Process results obtained asynchronously after a call to PQsendQuery, PQsendQueryParams,
 * PQsendPrepare, PQsendQueryPrepared, PQsendDescribePrepared, or PQsendDescribePortal.
 *
 * @param conn the connection to the PG server
 * @param res the intermediate libpq query result
 * @param singleRowMode if true, it activates single-row mode for the current query
 * @param pgResultConsumedPromise a callback, provided as Promise[Unit], which is triggered when all rows have been fully consumed
 */
class IterableQueryResult private[libpq4s](
  conn: IPGconn,
  res: IPGresult,
  singleRowMode: Boolean,
  pgResultConsumedPromise: Promise[Unit]
)(implicit libpq: ILibPQ) extends Logging {

  import libpq._

  private var _pgResult = res

  def isClosed: Boolean = _pgResult == null

  def close(): Unit = {
    if (!isClosed) {

      // TODO: should we cancel instead or do it only once (no while loop)???
      var resultStatusIsOK = true
      while (_pgResult != null && resultStatusIsOK) {
        PQclear(_pgResult)
        _pgResult = PQgetResult(conn)
        val resultStatus = PQresultStatus(_pgResult)
        resultStatusIsOK = resultStatus == PGRES_TUPLES_OK || resultStatus == PGRES_SINGLE_TUPLE
      }

      if (!pgResultConsumedPromise.isCompleted) {
        pgResultConsumedPromise.success( (): Unit )
      }
    }
  }

  lazy val queryResult = new QueryResult(res)

  // PQgetResult must be called repeatedly until it returns a null pointer, indicating that the command is done.
  // A null pointer is returned when the command is complete and there will be no more results.
  // Don't forget to free each result object with PQclear when done with it.
  // Note that PQgetResult will block only if a command is active and the necessary response data has not yet been read by PQconsumeInput.
  def foreachRow(onEachRow: IRowData[String] => Unit): Int = {
    require(!isClosed, "resource is closed")

    // FIXME: is it safe to do try/finally there? Are we sure that results will be consumed?
    val nRows = try {
      if (singleRowMode) _iterateRowsSingleMode(onEachRow)
      else {
        var totalReadRows = 0

        var i = 0
        // FIXME: in multiple rows mode PQgetResult() never returns NULL, so we check PQresultStatus() instead
        while (_pgResult != null && PQresultStatus(_pgResult) == PGRES_TUPLES_OK) {
          //println("_pgResult loop: "+i)

          // TODO: call PQnfields only once?
          try {
            totalReadRows += _iterateRowsMultipleMode(onEachRow)
          } finally {
            PQclear(_pgResult)
          }

          _pgResult = PQgetResult(conn)
          i += 1
        }

        totalReadRows
      }

    } finally {
      //println("pgResultConsumedPromise succeeded")
      this.close()
    }

    nRows
  }

  def _iterateRowsMultipleMode(onEachRow: IRowData[String] => Unit): Int = {
    if (_pgResult == null) {
      logger.debug("no results")
      return 0
    }

    //logger.debug("PQresultStatus: "+ PQresultStatus(_pgResult))

    // FIXME: enable me back when we fix _pgResult NEVER NULL issue
    /*if (PQresultStatus(_pgResult) != PGRES_TUPLES_OK) {
      PQclear(_pgResult)
      throw new Exception("No data retrieved: " + pq.PQresultErrorMessage(_pgResult))
    }*/

    val nRows = PQntuples(_pgResult)
    //logger.debug(s"number of rows returned = $nRows")
    val nFields = PQnfields(_pgResult)
    //logger.debug(s"number of fields returned = $nFields")

    val fieldMapping = _createFieldMapping(nFields)

    var i = 0
    while (i < nRows) {
      //logger.debug(s"i: $i")

      val fields = _parseRow(i, nFields)
      val row = new ArrayRowData(i, fields, fieldMapping)

      //logger.debug(s"row: $row")
      onEachRow(row)

      i += 1
    }

    nRows
  }

  def _iterateRowsSingleMode(onEachRow: IRowData[String] => Unit): Int = {
    if (_pgResult == null) return 0

    if (PQresultStatus(_pgResult) == PGRES_BAD_RESPONSE) {
      PQclear(_pgResult)
      throw new Exception("no data retrieved: " + PQresultErrorMessage(_pgResult))
    }

    val nFields = PQnfields(_pgResult)
    //logger.debug(s"number of fields returned = $nFields")

    val fieldMapping = _createFieldMapping(nFields)

    var allResultsProcessed = false
    var rowCount = 0
    while (_pgResult != null && !allResultsProcessed) {

      val resultStatus = PQresultStatus(_pgResult)
      if (resultStatus == PGRES_SINGLE_TUPLE) {
        val fields = _parseRow(0, nFields)
        val row = new ArrayRowData(rowCount, fields, fieldMapping)
        onEachRow(row)
        PQclear(_pgResult)
        rowCount += 1
      } else if (resultStatus == PGRES_TUPLES_OK) {
        PQclear(_pgResult)
        allResultsProcessed = true
      } else {
        PQclear(_pgResult)
        throw new Exception(s"bad PQresultStatus ($resultStatus): " + PQresultErrorMessage(_pgResult))
      }

      _pgResult = PQgetResult(conn)
    } // ends while (!allResultsProcessed)

    rowCount
  }

  private def _createFieldMapping(nFields: Int): collection.Map[String,Int] = {
    val fieldMapping = new collection.mutable.HashMap[String,Int]

    var i = 0
    while (i < nFields) {
      fieldMapping.put(PQfname(_pgResult, i), i)
      i += 1
    }

    fieldMapping
  }

  private def _parseRow(rowIndex: Int, nFields: Int): collection.Seq[String] = {

    val fields = new collection.mutable.ArrayBuffer[String](nFields)

    var i = 0
    while (i < nFields) {
      fields += PQgetvalue(_pgResult, rowIndex, i)/*FieldContent(
        PQfname(_pgResult, i),
        PQgetvalue(_pgResult, rowIndex, i),
        PQgetlength(_pgResult, rowIndex, i)
      )*/
      i += 1
    }

    fields
  }

}

/*
case class FieldContent(
  name: String,
  value: String,
  length: Int
)
 */

