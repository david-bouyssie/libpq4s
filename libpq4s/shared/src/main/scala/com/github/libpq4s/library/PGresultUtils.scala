package com.github.libpq4s.library

import com.github.libpq4s.api.IExecStatusType

private[libpq4s] object PGresultUtils {
  def checkExecutionStatus(pgConn: IPGconn, pgResult: IPGresult, expectedStatus: IExecStatusType, clearOnMismatch: Boolean = false)(implicit libpq: ILibPQ): Unit = {
    if (pgResult == null || libpq.PQresultStatus(pgResult) != expectedStatus) {
      val status = if (pgResult == null) "NULL" else {
        if (clearOnMismatch) libpq.PQclear(pgResult)
        libpq.PQresultStatus(pgResult).getName()
      }

      val errMsg = libpq.PQerrorMessage(pgConn)

      throw new Exception(s"query execution failed (status=$status) with error '$errMsg'")
    }
  }
}
