package com.github.libpq4s.api

/**
 * Represents a row from a database, allows clients to access rows by column number or column name.
 */
trait IRowData[T] extends IndexedSeq[T] {

  /**
   * Returns a column value by it's position in the originating query.
   *
   * @param columnNumber
   * @return
   */
  def apply( columnNumber: Int ): T

  /**
   * Returns a column value by it's name in the originating query.
   *
   * @param columnName
   * @return
   */
  def apply( columnName: String ): T

  /**
   * Index of this row in the query results. Counts start at 0.
   *
   * @return
   */
  def rowIndex: Int

}
