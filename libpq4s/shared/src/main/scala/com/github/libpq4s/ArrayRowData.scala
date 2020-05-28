package com.github.libpq4s

import com.github.libpq4s.api.IRowData

class ArrayRowData[T](
  /** Index of this row in the query results. Counts start at 0. */
  val rowIndex: Int,
  val fields: Seq[T],
  val fieldMapping: collection.Map[String, Int]
) extends IRowData[T] {

  /**
   *
   * Returns a column value by it's position in the originating query.
   *
   * @param columnNumber
   * @return
   */
  def apply(columnNumber: Int): T = fields(columnNumber)

  /**
   *
   * Returns a column value by it's name in the originating query.
   *
   * @param columnName
   * @return
   */
  def apply(columnName: String): T = fields( fieldMapping(columnName) )

  def length: Int = fields.length

  override def toString(): String = fields.mkString("\t")
}
