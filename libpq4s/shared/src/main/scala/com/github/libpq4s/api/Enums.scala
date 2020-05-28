package com.github.libpq4s.api

trait ILibPQEnumItem {
  def getValue(): Int
  def getName(): String
}

trait IPGVerbosity extends ILibPQEnumItem
trait IPGContextVisibility extends ILibPQEnumItem
trait IPGPing extends ILibPQEnumItem
trait IPGErrorFieldIdentifier extends ILibPQEnumItem

sealed abstract class LibPQEnum[T <: ILibPQEnumItem] {

  type Item <: T

  private val _items = new collection.mutable.ArrayBuffer[Item]()
  private var _curItemValue = - 1

  protected final def nextItemValue(): Int = {
    _curItemValue += 1
    _curItemValue
  }

  abstract class AbstractItem private[libpq4s](name: String) extends ILibPQEnumItem {
    _items += this.asInstanceOf[Item]

    private val value = nextItemValue()
    def getValue(): Int = this.value
    def getName(): String = this.name

    override def toString() = this.name
  }

  def withValue(value: Int): Option[Item] = {
    if (value < 0 || value >= _items.length) return None
    Some(_items(value))
  }

}

object PGVerbosity extends LibPQEnum[IPGVerbosity] {

  case class Item private[libpq4s](name: String) extends AbstractItem(name) with IPGVerbosity

  val PQERRORS_TERSE = Item("PQERRORS_TERSE")
  val PQERRORS_DEFAULT = Item("PQERRORS_DEFAULT")
  val PQERRORS_VERBOSE = Item("PQERRORS_VERBOSE")
}

object PGContextVisibility extends LibPQEnum[IPGContextVisibility] {

  case class Item private[libpq4s](name: String) extends AbstractItem(name) with IPGContextVisibility

  val PQSHOW_CONTEXT_NEVER = Item("PQSHOW_CONTEXT_NEVER")
  val PQSHOW_CONTEXT_ERRORS = Item("PQSHOW_CONTEXT_ERRORS")
  val PQSHOW_CONTEXT_ALWAYS = Item("PQSHOW_CONTEXT_ALWAYS")
}

object PGPing extends LibPQEnum[IPGPing]  {

  case class Item private[libpq4s](name: String) extends AbstractItem(name) with IPGPing

  /** The server is running and appears to be accepting connections. */
  val PQPING_OK = Item("PQPING_OK")
  /** The server is running but is in a state that disallows connections (startup, shutdown, or crash recovery). */
  val PQPING_REJECT = Item("PQPING_REJECT")
  /** The server could not be contacted.
   * This might indicate that the server is not running,
   * or that there is something wrong with the given connection parameters (for example, wrong port number),
   * or that there is a network connectivity problem (for example, a firewall blocking the connection request). */
  val PQPING_NO_RESPONSE = Item("PQPING_NO_RESPONSE")
  /** No attempt was made to contact the server, because the supplied parameters were obviously incorrect or there was some client-side problem (for example, out of memory). */
  val PQPING_NO_ATTEMPT = Item("PQPING_NO_ATTEMPT")
}


object PGErrorFieldIdentifier extends LibPQEnum[IPGErrorFieldIdentifier]  {

  case class Item private[libpq4s](name: String, value: Int) extends IPGErrorFieldIdentifier {
    def getName(): String = name
    def getValue(): Int = value
  }

  val PG_DIAG_SEVERITY = Item("PG_DIAG_SEVERITY",'S')
  val PG_DIAG_SEVERITY_NONLOCALIZED = Item("PG_DIAG_SEVERITY_NONLOCALIZED",'V')
  val PG_DIAG_SQLSTATE = Item("PG_DIAG_SQLSTATE",'C')
  val PG_DIAG_MESSAGE_PRIMARY = Item("PG_DIAG_MESSAGE_PRIMARY",'M')
  val PG_DIAG_MESSAGE_DETAIL = Item("PG_DIAG_MESSAGE_DETAIL",'D')
  val PG_DIAG_MESSAGE_HINT = Item("PG_DIAG_MESSAGE_HINT",'H')
  val PG_DIAG_STATEMENT_POSITION = Item("PG_DIAG_STATEMENT_POSITION",'P')
  val PG_DIAG_INTERNAL_POSITION = Item("PG_DIAG_INTERNAL_POSITION",'p')
  val PG_DIAG_INTERNAL_QUERY = Item("PG_DIAG_INTERNAL_QUERY",'q')
  val PG_DIAG_CONTEXT = Item("PG_DIAG_CONTEXT",'W')
  val PG_DIAG_SCHEMA_NAME = Item("PG_DIAG_SCHEMA_NAME",'s')
  val PG_DIAG_TABLE_NAME = Item("PG_DIAG_TABLE_NAME",'t')
  val PG_DIAG_COLUMN_NAME = Item("PG_DIAG_COLUMN_NAME",'c')
  val PG_DIAG_DATATYPE_NAME = Item("PG_DIAG_DATATYPE_NAME",'d')
  val PG_DIAG_CONSTRAINT_NAME = Item("PG_DIAG_CONSTRAINT_NAME",'n')
  val PG_DIAG_SOURCE_FILE = Item("PG_DIAG_SOURCE_FILE",'F')
  val PG_DIAG_SOURCE_LINE = Item("PG_DIAG_SOURCE_LINE",'L')
  val PG_DIAG_SOURCE_FUNCTION = Item("PG_DIAG_SOURCE_FUNCTION",'R')
}

/*
sealed abstract class LibPQEnum extends Enumeration {

  private var _curStatusValue = -1

  protected final def Item(name: String): Item = {
    _curStatusValue += 1
    new Item(_curStatusValue, name)
  }

  class Item private[StatusType](value: Int, name: String) extends Val(value, name) with ILibPQEnumItem {
    def getValue(): Int = this.value
    def getName(): String = this.name
  }

  final def withItemValue(value: Int): Item = this.apply(value).asInstanceOf[Item]
  final def withItemName(name: String): Item = super.withName(name).asInstanceOf[Item]
}

object PGVerbosity extends LibPQEnum {
  val PQERRORS_TERSE = Item("PQERRORS_TERSE")
  val PQERRORS_DEFAULT = Item("PQERRORS_DEFAULT")
  val PQERRORS_VERBOSE = Item("PQERRORS_VERBOSE")
}

object PGContextVisibility extends LibPQEnum {
  val PQSHOW_CONTEXT_NEVER = Item("PQSHOW_CONTEXT_NEVER")
  val PQSHOW_CONTEXT_ERRORS = Item("PQSHOW_CONTEXT_ERRORS")
  val PQSHOW_CONTEXT_ALWAYS = Item("PQSHOW_CONTEXT_ALWAYS")
}

object PGPing extends LibPQEnum {
  val PQPING_OK = Item("PQPING_OK")
  val PQPING_REJECT = Item("PQPING_REJECT")
  val PQPING_NO_RESPONSE = Item("PQPING_NO_RESPONSE")
  val PQPING_NO_ATTEMPT = Item("PQPING_NO_ATTEMPT")
}
*/