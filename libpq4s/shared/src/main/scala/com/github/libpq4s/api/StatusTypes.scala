package com.github.libpq4s.api

sealed trait IStatusType {
  def getCode(): Int
  def getName(): String
}

trait IConnStatusType extends IStatusType
trait IExecStatusType extends IStatusType
trait ITransactionStatusType extends IStatusType
trait IPostgresPollingStatusType extends IStatusType

/*sealed abstract class StatusType extends Enumeration {

  private var _curStatusValue = -1

  protected final def Status(name: String): Status = {
    _curStatusValue += 1
    new Status(_curStatusValue, name)
  }

  class Status private[StatusType](value: Int, name: String) extends Val(value, name) with IStatusType {
    def getValue(): Int = this.value
  }

  final def withValue(value: Int): Status = this.apply(value).asInstanceOf[Status]
}*/

sealed abstract class StatusType[T <: IStatusType] {

  type Status <: T

  private val _statusTypes = new collection.mutable.ArrayBuffer[Status]()
  private var _curStatusCode = - 1

  protected final def nextStatusCode(): Int = {
    _curStatusCode += 1
    _curStatusCode
  }

  abstract class AbstractStatus private[libpq4s](name: String) extends IStatusType {
    _statusTypes += this.asInstanceOf[Status]

    private val code = nextStatusCode()
    def getCode(): Int = this.code
    def getName(): String = this.name

    override def toString() = this.name
  }

  def withCode(code: Int): Option[Status] = {
    if (code < 0 || code >= _statusTypes.length) return None
    Some(_statusTypes(code))
  }
}

object ConnStatusType extends StatusType[IConnStatusType] {

  case class Status private[libpq4s](name: String) extends AbstractStatus(name) with IConnStatusType

  val CONNECTION_OK: IStatusType = Status("CONNECTION_OK")
  val CONNECTION_BAD: IStatusType = Status("CONNECTION_BAD")
  val CONNECTION_STARTED: IStatusType = Status("CONNECTION_STARTED")
  val CONNECTION_MADE: IStatusType = Status("CONNECTION_MADE")
  val CONNECTION_AWAITING_RESPONSE: IStatusType = Status("CONNECTION_AWAITING_RESPONSE")
  val CONNECTION_AUTH_OK: Status = Status("CONNECTION_AUTH_OK")
  val CONNECTION_SETENV: Status = Status("CONNECTION_SETENV")
  val CONNECTION_SSL_STARTUP: Status = Status("CONNECTION_SSL_STARTUP")
  val CONNECTION_NEEDED: Status = Status("CONNECTION_NEEDED")
  val CONNECTION_CHECK_WRITABLE: Status = Status("CONNECTION_CHECK_WRITABLE")
  val CONNECTION_CONSUME: Status = Status("CONNECTION_CONSUME")
}

object ExecStatusType extends StatusType[IExecStatusType] {

  case class Status private[libpq4s](name: String) extends AbstractStatus(name) with IExecStatusType

  val PGRES_EMPTY_QUERY: Status = Status("PGRES_EMPTY_QUERY")
  val PGRES_COMMAND_OK: Status = Status("PGRES_COMMAND_OK")
  val PGRES_TUPLES_OK: Status = Status("PGRES_TUPLES_OK")
  val PGRES_COPY_OUT: Status = Status("PGRES_COPY_OUT")
  val PGRES_COPY_IN: Status = Status("PGRES_COPY_IN")
  val PGRES_BAD_RESPONSE: Status = Status("PGRES_BAD_RESPONSE")
  val PGRES_NONFATAL_ERROR: Status = Status("PGRES_NONFATAL_ERROR")
  val PGRES_FATAL_ERROR: Status = Status("PGRES_FATAL_ERROR")
  val PGRES_COPY_BOTH: Status = Status("PGRES_COPY_BOTH")
  val PGRES_SINGLE_TUPLE: Status = Status("PGRES_SINGLE_TUPLE")
}

object TransactionStatusType extends StatusType[ITransactionStatusType] {

  case class Status private[libpq4s](name: String) extends AbstractStatus(name) with ITransactionStatusType

  val PQTRANS_IDLE: Status = Status("PQTRANS_IDLE")
  val PQTRANS_ACTIVE: Status = Status("PQTRANS_ACTIVE")
  val PQTRANS_INTRANS: Status = Status("PQTRANS_INTRANS")
  val PQTRANS_INERROR: Status = Status("PQTRANS_INERROR")
  val PQTRANS_UNKNOWN: Status = Status("PQTRANS_UNKNOWN")
}

object PostgresPollingStatusType extends StatusType[IPostgresPollingStatusType] {

  case class Status private[libpq4s](name: String) extends AbstractStatus(name) with IPostgresPollingStatusType

  val PGRES_POLLING_FAILED: Status = Status("PGRES_POLLING_FAILED")
  val PGRES_POLLING_READING: Status = Status("PGRES_POLLING_READING")
  val PGRES_POLLING_WRITING: Status = Status("PGRES_POLLING_WRITING")
  val PGRES_POLLING_OK: Status = Status("PGRES_POLLING_OK")
  val PGRES_POLLING_ACTIVE: Status = Status("PGRES_POLLING_ACTIVE")
}