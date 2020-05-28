package com.github.libpq4s.library

import scala.language.implicitConversions
import scala.scalanative.unsafe._
import scala.scalanative.unsigned._

import com.github.libpq4s.api._
import com.github.libpq4s.bindings.pq

object LibPQ extends ILibPQ {

  implicit class FileWrapper(val ptr: Ptr[pq.FILE]) extends AnyVal with IFile

  //type OidPtr = Ptr[pq.Oid] with IOid
  //private implicit def oid2oidPtr(oid: IOid): OidPtr = oid.asInstanceOf[OidPtr]
  //private implicit def pgcancelPtr2pgcancel(ptr: Ptr[pq.PGcancel]): PGcancel = ptr.asInstanceOf[PGcancel]
  implicit class OidWrapper(val ptr: Ptr[pq.Oid]) extends AnyVal with IOidBox {
    def toInt(): Int = (!ptr).toInt
  }

  implicit class OidBox(oid: pq.Oid) extends IOidBox {
    def toInt(): Int = oid.toInt
  }

  //type PGcancelPtr = Ptr[pq.PGcancel] with IPGcancelPtr
  implicit class PGcancelWrapper(val ptr: Ptr[pq.PGcancel]) extends AnyVal with IPGcancel
  //private implicit def pgcancelPtr2pgcancel(ptr: Ptr[pq.PGcancel]): PGcancelPtr = ptr.asInstanceOf[PGcancelPtr]
  private implicit def pgcancel2pgcancelPtr(pgcancel: IPGcancel): Ptr[pq.PGcancel] = pgcancel.asInstanceOf[PGcancelWrapper].ptr

  //type PGconnPtr = Ptr[pq.PGconn] with IPGconnPtr
  //private implicit def pgconn2pgconnPtr(pgconn: IPGconn): PGconnPtr = pgconn.asInstanceOf[PGconnPtr]
  //private implicit def pgconnPtr2pgconn(ptr: Ptr[pq.PGconn]): PGconnPtr = ptr.asInstanceOf[PGconnPtr]
  class PGconnWrapper(val ptr: Ptr[pq.PGconn]) extends AnyVal with IPGconn
  private implicit def pgconnPtr2pgconn(ptr: Ptr[pq.PGconn]): IPGconn = new PGconnWrapper(ptr)
  private implicit def pgconn2pgconnPtr(pgconn: IPGconn): Ptr[pq.PGconn] = pgconn.asInstanceOf[PGconnWrapper].ptr
  //private implicit def pgconn2pgconnPtr(pgresult: IPGconn): Ptr[pq.PGconn] = pgresult.asInstanceOf[Ptr[pq.PGconn]]
  /*private implicit def pgconn2pgconnPtr(pgconn: IPGconn): Ptr[pq.PGconn] = {
    pgconn.asInstanceOf[Any] match {
      case pgconnWrapper: PGconnWrapper => pgconnWrapper.ptr
      case pgconnPtr: Ptr[pq.PGconn] => pgconnPtr
      case _ => throw new Exception("can't cast 'pgconn: IPGconn' as a 'Ptr[pq.PGconn]'")
    }
  }*/

  //type PGresultPtr = Ptr[pq.PGresult] with IPGresultPtr
  //private implicit def pgresult2pgresultPtr(pgresult: IPGresult): PGresultPtr = pgresult.asInstanceOf[PGresultPtr]
  //private implicit def pgresultPtr2pgresult(ptr: Ptr[pq.PGresult]): PGresultPtr = ptr.asInstanceOf[PGresultPtr]
  class PGresultWrapper(val ptr: Ptr[pq.PGresult]) extends AnyVal with IPGresult
  private implicit def pgresultPtr2pgresult(ptr: Ptr[pq.PGresult]): IPGresult = new PGresultWrapper(ptr)
  private implicit def pgresult2pgresultPtr(pgresult: IPGresult): Ptr[pq.PGresult] = pgresult.asInstanceOf[PGresultWrapper].ptr
  //private implicit def pgresult2pgresultPtr(pgresult: IPGresult): Ptr[pq.PGresult] = pgresult.asInstanceOf[Ptr[pq.PGresult]]


  // FIXME: moved from pq.scala to here to avoid the error "methods in extern objects must have extern body"
  // TODO: determine of it is an SN bug or the expected behavior
  implicit class PQconninfoOptionWrapper(val p: Ptr[pq.PQconninfoOption]) extends AnyVal with IPQconninfoOptionWrapper {

    def keyword: CString = p._1
    //def keyword_=(value: CString): Unit = !p._1 = value
    def envvar: CString = p._2
    //def envvar_=(value: CString): Unit = !p._2 = value
    def compiled: CString = p._3
    //def compiled_=(value: CString): Unit = !p._3 = value
    def `val`: CString = p._4
    //def `val_=`(value: CString): Unit = !p._4 = value
    def label: CString = p._5
    //def label_=(value: CString): Unit = !p._5 = value
    def dispchar: CString = p._6
    //def dispchar_=(value: CString): Unit = !p._6 = value
    def dispsize: CInt = p._7
    //def dispsize_=(value: CInt): Unit = !p._7 = value

    @inline def free(): Unit = {
      if (p != null) PQconninfoFree(p)
      //!p = null // FIXME: is it safe?
    }

    private def _toConnInfoOption(): ConnInfoOption = {
      /*if (p == null) return None
      if (!p == null) return None
      if (keyword == null) return None*/

      val connOption = ConnInfoOption(
        fromCString(keyword),
        fromCString(envvar),
        fromCString(compiled),
        if (`val` == null ) None else Some(fromCString(`val`)),
        fromCString(label),
        dispchar.apply(0).toChar,
        dispsize
      )

      connOption
    }

    def forEachConnInfoOption(fn: ConnInfoOption => Unit): Unit = {
      if (p == null) throw new Exception("can't allocate memory for PQconninfoOption")

      var reachedNullPtr = false
      while (!reachedNullPtr) {
        if (!p == null || keyword == null) reachedNullPtr = true
        else {
          fn(_toConnInfoOption())
        }
      }

      ()
    }

  }


  // Convert Seq[String] to NULL-terminated arrays typed as Ptr[CString]
  private def strs2cstrPtr(strings: Seq[String], appendNullStr: Boolean = false)(implicit z: Zone): Ptr[CString] = {
    val nStrings = if (appendNullStr) strings.length + 1 else strings.length
    val cstrBuffer = alloc[CString](nStrings)

    var i = 0
    while (i < strings.length) {
      val str = strings(i)
      val cstr = if (str == null) null else toCString(str)
      cstrBuffer.update(i, cstr)
      i += 1
    }

    if (appendNullStr)
      cstrBuffer.update(i, null)

    cstrBuffer
  }

  // Convert Seq[Array[Byte]] to binary arrays typed as Ptr[CString]
  private def byteArrays2cstrPtr(byteArrays: Seq[Array[Byte]])(implicit z: Zone): Ptr[CString] = {
    val nArrays = byteArrays.length
    val cstrBuffer = alloc[CString](nArrays)

    var i = 0
    while (i < nArrays) {
      val bytes: Ptr[Byte] = byteArrays(i).asInstanceOf[scala.scalanative.runtime.ByteArray].at(0)
      cstrBuffer.update(i, bytes)
      i += 1
    }

    cstrBuffer
  }

  import scala.scalanative.runtime.IntArray

  def intArray2intPtr(arr: Array[Int])(implicit z: Zone): Ptr[CInt] = {
    if (arr == null) return null

    arr.asInstanceOf[IntArray].at(0)
  }

  def oidSeq2oidPtr(arr: Seq[IOidBox])(implicit z: Zone): Ptr[pq.Oid] = {
    if (arr == null) return null

    val nOids = arr.length
    val oidPtr = z.alloc(sizeof[pq.Oid] * nOids).asInstanceOf[Ptr[pq.Oid]]

    var c = 0
    while (c < nOids) {
      val oid = arr(c).asInstanceOf[OidWrapper].ptr
      !(oidPtr + c) = !oid
      c += 1
    }

    oidPtr
  }

  // TODO: move below (duplicated)
  @inline def PQclear(res: IPGresult): Unit = {
    require(res != null, "res is null")
    pq.PQclear(res)
  }

  def PQconnectStart(conninfo: String): IPGconn = {
    require(conninfo != null, "conninfo is null")
    Zone { implicit z =>
      pq.PQconnectStart(toCString(conninfo))
    }
  }

  // TODO: check me => complex API
  // TODO
  def PQconnectStartParams(params: Seq[(String,String)], expandDbName: Boolean): IPGconn = {
    require(params != null, "params is null")

    val keywords = params.map(_._1)
    val values = params.map(_._2)

    Zone { implicit z =>
      val cKeywords = strs2cstrPtr(keywords, appendNullStr = true)
      val cValues = strs2cstrPtr(values, appendNullStr = true)

      pq.PQconnectStartParams(cKeywords, cValues, if (expandDbName) 1 else 0)
    }
  }

  def PQconnectPoll(conn: IPGconn): IPostgresPollingStatusType = {
    require(conn != null, "conn is null")

    val statusTypeCode = pq.PQconnectPoll(conn).toInt
    PostgresPollingStatusType
      .withCode(statusTypeCode)
      .getOrElse(throw new Exception(s"invalid PostgresPollingStatusType code: $statusTypeCode"))
  }

  def PQconnectdb(conninfo: String): IPGconn = {
    require(conninfo != null, "conninfo is null")
    Zone { implicit z =>
      pq.PQconnectdb(toCString(conninfo))
    }
  }

  // TODO: check me => complex API
  def PQconnectdbParams(params: Seq[(String,String)], expandDbName: Boolean): IPGconn = {
    require(params != null, "params is null")

    val keywords = params.map(_._1)
    val values = params.map(_._2)

    Zone { implicit z =>
      val cKeywords = strs2cstrPtr(keywords, appendNullStr = true)
      val cValues = strs2cstrPtr(values, appendNullStr = true)

      pq.PQconnectdbParams(cKeywords, cValues, if (expandDbName) 1 else 0)
    }
  }

  def PQsetdbLogin(pghost: String, pgport: String, pgoptions: String, pgtty: String, dbName: String, login: String, pwd: String): IPGconn = {
    implicit def str2cstr(str: String)(implicit z: Zone): CString = CUtils.toCString(str)
    Zone { implicit z =>
      pq.PQsetdbLogin(pghost, pgport, pgoptions, pgtty, dbName, login, pwd)
    }
  }

  @inline def PQfinish(conn: IPGconn): Unit = {
    require(conn != null, "conn is null")
    pq.PQfinish(conn)
  }

  @inline def PQconndefaults(): IPQconninfoOptionWrapper = pq.PQconndefaults()

  // def PQconninfoParse(conninfo: String, errmsg: Seq[String]): IPQconninfoOptionWrapper = {
  def PQconninfoParse(conninfo: String): util.Try[IPQconninfoOptionWrapper] = {
    require(conninfo != null, "conninfo is null")

    Zone { implicit z =>
      val cErrMsgPtr = stackalloc[CString]
      !cErrMsgPtr = null

      val connOptPtr = pq.PQconninfoParse(toCString(conninfo), cErrMsgPtr)

      if (cErrMsgPtr != null && cErrMsgPtr(0) != null) {
        val errMsg = fromCString(!cErrMsgPtr)
        pq.PQfreemem(cErrMsgPtr.asInstanceOf[Ptr[Byte]])
        util.Failure(new Exception(s"can't retrieve connection options, because $errMsg"))
      }

      util.Success(connOptPtr)
    }
  }

  @inline def PQconninfo(conn: IPGconn): IPQconninfoOptionWrapper = {
    require(conn != null, "conn is null")
    pq.PQconninfo(conn)
  }

  @inline def PQconninfoFree(connOptions: IPQconninfoOptionWrapper): Unit = {
    connOptions.free() // PQconninfoFree is be called internally by the IPQconninfoOptionWrapper
    //pq.PQconninfoFree(connOptions.p)
  }

  // TODO
  @inline def PQresetStart(conn: IPGconn): Boolean = {
    require(conn != null, "conn is null")
    if (pq.PQresetStart(conn) == 0) false else true
  }

  // TODO
  def PQresetPoll(conn: IPGconn): IPostgresPollingStatusType = {
    require(conn != null, "conn is null")

    val statusTypeCode = pq.PQresetPoll(conn).toInt
    PostgresPollingStatusType
      .withCode(statusTypeCode)
      .getOrElse(throw new Exception(s"invalid PostgresPollingStatusType code: $statusTypeCode"))
  }

  @inline def PQreset(conn: IPGconn): Unit = {
    require(conn != null, "conn is null")
    pq.PQreset(conn)
  }

  @inline def PQgetCancel(conn: IPGconn): IPGcancel = {
    require(conn != null, "conn is null")
    pq.PQgetCancel(conn)
  }

  @inline def PQfreeCancel(cancel: IPGcancel): Unit = {
    require(cancel != null, "conn is null")
    pq.PQfreeCancel(cancel)
  }

  //def PQcancel(cancel: IPGcancel, errbuf: String, errbufsize: Int): Int = {
  def PQcancel(cancel: IPGcancel, errbufsize: Int = 512): util.Try[Unit] = {
    require(cancel != null, "cancel is null")

    val errbufsize = 512
    val errbuf = scalanative.libc.stdlib.malloc(errbufsize) //.asInstanceOf[Ptr[CChar]]

    val isOk = pq.PQcancel(cancel, errbuf, errbufsize)

    val cancelResult = if (isOk == 1) util.Success(())
    else {
      val errMsg = if (errbuf == null) "" else fromCString(errbuf)
      util.Failure(new Exception(errMsg))
    }

    cancelResult
  }

  //@deprecated def PQrequestCancel(conn: IPGconn): Int

  @inline def PQdb(conn: IPGconn): String = { require(conn != null, "conn is null"); fromCString(pq.PQdb(conn)) }
  @inline def PQuser(conn: IPGconn): String = { require(conn != null, "conn is null"); fromCString(pq.PQuser(conn)) }
  @inline def PQpass(conn: IPGconn): String = { require(conn != null, "conn is null"); fromCString(pq.PQpass(conn)) }
  @inline def PQhost(conn: IPGconn): String = { require(conn != null, "conn is null"); fromCString(pq.PQhost(conn)) }
  @inline def PQport(conn: IPGconn): String = { require(conn != null, "conn is null"); fromCString(pq.PQport(conn)) }
  /* Returns the debug TTY of the connection (obsolete but kept for backwards compatibility). */
  //@deprecated def PQtty(conn: IPGconn): String = { require(conn != null, "conn is null"); fromCString(pq.PQtty(conn)) }
  @inline def PQoptions(conn: IPGconn): String = { require(conn != null, "conn is null"); fromCString(pq.PQoptions(conn)) }

  @inline def PQstatus(conn: IPGconn): IConnStatusType = {
    require(conn != null, "conn is null")

    val statusTypeCode = pq.PQstatus(conn).toInt
    ConnStatusType
      .withCode(statusTypeCode)
      .getOrElse(throw new Exception(s"invalid ConnStatusType code: $statusTypeCode"))
  }

  def PQtransactionStatus(conn: IPGconn): ITransactionStatusType = {
    require(conn != null, "conn is null")

    val statusTypeCode = pq.PQtransactionStatus(conn).toInt
    TransactionStatusType
      .withCode(statusTypeCode)
      .getOrElse(throw new Exception(s"invalid TransactionStatusType code: $statusTypeCode"))
  }

  def PQparameterStatus(conn: IPGconn, paramName: String): String = {
    require(conn != null, "conn is null")
    require(paramName != null, "paramName is null")

    Zone { implicit z =>
      val cParamValue = pq.PQparameterStatus(conn, toCString(paramName))
      if (cParamValue == null) null else fromCString(cParamValue)
    }
  }

  @inline def PQlibVersion(): Int = pq.PQlibVersion()

  @inline def PQprotocolVersion(conn: IPGconn): Int = {
    require(conn != null, "conn is null")
    pq.PQprotocolVersion(conn)
  }

  @inline def PQserverVersion(conn: IPGconn): Int = {
    require(conn != null, "conn is null")
    pq.PQserverVersion(conn)
  }

  @inline def PQerrorMessage(conn: IPGconn): String = {
    require(conn != null, "conn is null")
    val cErrMsg = pq.PQerrorMessage(conn)
    if (cErrMsg == null) "" else fromCString(cErrMsg)
  }

  @inline def PQsocket(conn: IPGconn): Int = {
    require(conn != null, "conn is null")
    pq.PQsocket(conn)
  }

  @inline def PQbackendPID(conn: IPGconn): Int = {
    require(conn != null, "conn is null")
    pq.PQbackendPID(conn)
  }

  @inline def PQconnectionNeedsPassword(conn: IPGconn): Boolean = {
    require(conn != null, "conn is null")
    if (pq.PQconnectionNeedsPassword(conn) == 0) false else true
  }

  @inline def PQconnectionUsedPassword(conn: IPGconn): Boolean = {
    require(conn != null, "conn is null")
    if (pq.PQconnectionUsedPassword(conn) == 0) false else true
  }

  @inline def PQclientEncoding(conn: IPGconn): Int = {
    require(conn != null, "conn is null")
    pq.PQclientEncoding(conn)
  }

  def PQsetClientEncoding(conn: IPGconn, encoding: String): Boolean = {
    require(conn != null, "conn is null")
    require(encoding != null, "encoding is null")

    Zone { implicit z =>
      if (pq.PQsetClientEncoding(conn, toCString(encoding)) == -1) false else true
    }
  }

  /*def PQsslInUse(conn: IPGconn): Int
  def PQsslStruct(conn: IPGconn, struct_name: String): Ptr[Byte]
  def PQsslAttribute(conn: IPGconn, attribute_name: String): String
  def PQsslAttributeNames(conn: IPGconn): Seq[String]
  def PQgetssl(conn: IPGconn): Ptr[Byte]
  def PQinitSSL(do_init: Int): Unit
  def PQinitOpenSSL(do_ssl: Int, do_crypto: Int): Unit
  */

  def PQsetErrorVerbosity(conn: IPGconn, verbosity: IPGVerbosity): IPGVerbosity = {
    require(conn != null, "conn is null")

    val oldValue = pq.PQsetErrorVerbosity(conn, verbosity.getValue().toUInt)
    PGVerbosity.withValue(oldValue.toInt).getOrElse(throw new Exception(s"invalid PGVerbosity value: $oldValue"))
  }

  def PQsetErrorContextVisibility(conn: IPGconn, showContext: IPGContextVisibility): IPGContextVisibility = {
    require(conn != null, "conn is null")

    val oldValue = pq.PQsetErrorContextVisibility(conn, showContext.getValue().toUInt)
    PGContextVisibility.withValue(oldValue.toInt).getOrElse(throw new Exception(s"invalid PGContextVisibility value: $oldValue"))
  }

  /*@inline def PQtrace(conn: IPGconn, debugPort: IFile): Unit = {
    require(conn != null, "conn is null")
    pq.PQtrace(conn, debugPort)
  }

  @inline def PQuntrace(conn: IPGconn): Unit = {
    require(conn != null, "conn is null")
    pq.PQuntrace(conn)
  }*/

  //def PQsetNoticeReceiver(conn: IPGconn, proc: CFuncPtr2[Ptr[Byte], IPGresult, Unit], arg: Ptr[Byte]): CFuncPtr2[Ptr[Byte], IPGresult, Unit]
  //def PQsetNoticeProcessor(conn: IPGconn, proc: CFuncPtr2[Ptr[Byte], String, Unit], arg: Ptr[Byte]): CFuncPtr2[Ptr[Byte], String, Unit]

  def PQexec(conn: IPGconn, query: String): IPGresult = {
    require(conn != null, "conn is null")
    require(query != null, "query is null")

    Zone { implicit z =>
      pq.PQexec(conn, toCString(query))
    }
  }

  /*def PQexecParams(
    conn: IPGconn,
    command: String,
    nParams: Int,
    paramTypes: Seq[IOidBox],
    paramValues: Seq[String],
    paramLengths: Array[Int],
    paramFormats: Array[Int],
    resultFormat: Int
  ): IPGresult = {
    require(conn != null, "conn is null")
    require(command != null, "command is null")
    require(nParams >= 0, "invalid nParams value")

    if (paramTypes != null)
      require(paramTypes.length == nParams, s"paramTypes length is invalid, expected $nParams but is ${paramTypes.length}")
    if (paramValues != null)
      require(paramValues.length == nParams, s"paramValues length is invalid, expected $nParams but is ${paramValues.length}")
    if (paramLengths != null)
      require(paramLengths.length == nParams, s"paramLengths length is invalid, expected $nParams but is ${paramLengths.length}")
    if (paramFormats != null)
      require(paramFormats.length == nParams, s"paramFormats length is invalid, expected $nParams but is ${paramFormats.length}")

    Zone { implicit z =>
      val cCommand = toCString(command)
      val cParamTypes = if (paramTypes == null || nParams == 0) null else oidSeq2oidPtr(paramTypes)
      val cParamValues = if (paramValues == null || nParams == 0) strs2cstrPtr(paramValues)
      val cParamLengths = if (paramLengths == null || nParams == 0) null else intArray2intPtr(paramLengths)
      val cParamFormats = if (paramFormats == null || nParams == 0) null else intArray2intPtr(paramFormats)

      pq.PQexecParams(conn,cCommand,nParams,cParamTypes,cParamValues,cParamLengths,cParamFormats,resultFormat)
    }
  }*/

  def PQexecParamsText(
    conn: IPGconn,
    command: String,
    paramValues: Seq[String],
    paramTypes: Seq[IOidBox]
  ): IPGresult = {
    require(conn != null, "conn is null")
    require(command != null, "command is null")

    val nParams = if (paramValues == null || paramValues.isEmpty) 0 else paramValues.length

    if (paramTypes != null)
      require(paramTypes.length == nParams, s"paramTypes length is invalid, expected $nParams but is ${paramTypes.length}")

    Zone { implicit z =>
      val cCommand = toCString(command)
      val cParamValues = if (nParams == 0) null else strs2cstrPtr(paramValues)
      val cParamTypes = if (nParams == 0 || paramTypes == null) null else oidSeq2oidPtr(paramTypes)

      pq.PQexecParams(conn, cCommand, nParams, cParamTypes, cParamValues, paramLengths = null, paramFormats = null, resultFormat = 0)
    }
  }

  def PQexecParamsBinary(
    conn: IPGconn,
    command: String,
    paramValues: Seq[Array[Byte]],
    paramTypes: Seq[IOidBox]
  ): IPGresult = {
    require(conn != null, "conn is null")
    require(command != null, "command is null")

    val nParams = if (paramValues == null || paramValues.isEmpty) 0 else paramValues.length

    if (paramTypes != null)
      require(paramTypes.length == nParams, s"paramTypes length is invalid, expected $nParams but is ${paramTypes.length}")

    val paramFormats = if (nParams == 0) null else Array.fill[Int](nParams)(1)
    val paramLengths = paramValues.map(_.length).toArray

    Zone { implicit z =>
      val cCommand = toCString(command)
      val cParamTypes = if (nParams == 0 || paramTypes == null) null else oidSeq2oidPtr(paramTypes)
      val cParamValues = if (nParams == 0) null else byteArrays2cstrPtr(paramValues)
      val cParamLengths = if (nParams == 0) null else intArray2intPtr(paramLengths)
      val cParamFormats = if (nParams == 0) null else intArray2intPtr(paramFormats)

      pq.PQexecParams(conn, cCommand, nParams, cParamTypes, cParamValues, cParamLengths, cParamFormats, resultFormat = 1)
    }
  }

  def PQprepare(
    conn: IPGconn,
    stmtName: String,
    query: String,
    paramTypes: Seq[IOidBox]
  ): IPGresult = {
    require(conn != null, "conn is null")
    require(stmtName != null, "stmtName is null")
    require(query != null, "query is null")

    Zone { implicit z =>
      val cStmtName = toCString(stmtName)
      val cQuery = toCString(query)
      val nParams = if (paramTypes == null || paramTypes.isEmpty) 0 else paramTypes.length
      val cParamTypes = if (nParams == 0) null else oidSeq2oidPtr(paramTypes)
      pq.PQprepare(conn,cStmtName,cQuery,nParams,cParamTypes)
    }
  }

  /*def PQexecPrepared(
    conn: IPGconn,
    stmtName: String,
    nParams: Int,
    paramValues: Seq[Array[Byte]],
    paramLengths: Array[Int],
    paramFormats: Array[Int],
    resultFormat: Int
  ): IPGresult = {
    require(conn != null, "conn is null")
    require(stmtName != null, "stmtName is null")

    if (paramValues != null)
      require(paramValues.length == nParams, s"paramValues length is invalid, expected $nParams but is ${paramValues.length}")
    if (paramLengths != null)
      require(paramLengths.length == nParams, s"paramLengths length is invalid, expected $nParams but is ${paramLengths.length}")
    if (paramFormats != null)
      require(paramFormats.length == nParams, s"paramFormats length is invalid, expected $nParams but is ${paramFormats.length}")

    Zone { implicit z =>
      val cStmtName = toCString(stmtName)
      val cParamValues = if (paramValues == null || nParams == 0) null else strs2cstrPtr(paramValues)
      val cParamLengths = if (paramLengths == null || nParams == 0) null else intArray2intPtr(paramLengths)
      val cParamFormats = if (paramFormats == null || nParams == 0) null else intArray2intPtr(paramFormats)

      pq.PQexecPrepared(conn,cStmtName,nParams,cParamValues,cParamLengths,cParamFormats,resultFormat)
    }
  }*/

  def PQexecPreparedText(
    conn: IPGconn,
    stmtName: String,
    paramValues: Seq[String]
  ): IPGresult = {
    require(conn != null, "conn is null")
    require(stmtName != null, "stmtName is null")

    val nParams = if (paramValues == null || paramValues.isEmpty) 0 else paramValues.length

    Zone { implicit z =>
      val cStmtName = toCString(stmtName)
      val cParamValues = if (nParams == 0) null else strs2cstrPtr(paramValues)

      pq.PQexecPrepared(conn, cStmtName, nParams, cParamValues, paramLengths = null, paramFormats = null, resultFormat = 0)
    }
  }

  def PQexecPreparedBinary(
    conn: IPGconn,
    stmtName: String,
    paramValues: Seq[Array[Byte]]
  ): IPGresult = {
    require(conn != null, "conn is null")
    require(stmtName != null, "stmtName is null")

    val nParams = if (paramValues == null || paramValues.isEmpty) 0 else paramValues.length
    val paramFormats = if (nParams == 0) null else Array.fill[Int](nParams)(1)
    val paramLengths = paramValues.map(_.length).toArray

    Zone { implicit z =>
      val cStmtName = toCString(stmtName)
      val cParamValues = if (nParams == 0) null else byteArrays2cstrPtr(paramValues)
      val cParamLengths = if (nParams == 0) null else intArray2intPtr(paramLengths)
      val cParamFormats = if (nParams == 0) null else intArray2intPtr(paramFormats)

      pq.PQexecPrepared(conn, cStmtName, nParams, cParamValues, cParamLengths, cParamFormats, resultFormat = 1)
    }
  }

  def PQsendQuery(conn: IPGconn, query: String): Boolean = {
    require(conn != null, "conn is null")
    require(query != null, "query is null")

    Zone { implicit z =>
      val querySent = pq.PQsendQuery(conn, toCString(query))
      if (querySent == 0) false else true
    }
  }

  // TODO
  //def PQsendQueryParams(conn: IPGconn, command: String, nParams: Int, paramTypes: IOid, paramValues: Seq[String], paramLengths: Array[Int], paramFormats: Array[Int], resultFormat: Int): Int
  def PQsendQueryParamsText(
    conn: IPGconn,
    command: String,
    paramValues: Seq[String],
    paramTypes: Seq[IOidBox]
  ): Boolean = {
    require(conn != null, "conn is null")
    require(command != null, "command is null")

    val nParams = if (paramValues == null || paramValues.isEmpty) 0 else paramValues.length

    if (paramTypes != null)
      require(paramTypes.length == nParams, s"paramTypes length is invalid, expected $nParams but is ${paramTypes.length}")

    Zone { implicit z =>
      val cCommand = toCString(command)
      val cParamValues = if (nParams == 0) null else strs2cstrPtr(paramValues)
      val cParamTypes = if (nParams == 0 || paramTypes == null) null else oidSeq2oidPtr(paramTypes)

      val querySent = pq.PQsendQueryParams(conn, cCommand, nParams, cParamTypes, cParamValues, paramLengths = null, paramFormats = null, resultFormat = 0)

      if (querySent == 0) false else true
    }
  }

  def PQsendQueryParamsBinary(
    conn: IPGconn,
    command: String,
    paramValues: Seq[Array[Byte]],
    paramTypes: Seq[IOidBox]
  ): Boolean = {
    require(conn != null, "conn is null")
    require(command != null, "command is null")

    val nParams = if (paramValues == null || paramValues.isEmpty) 0 else paramValues.length

    if (paramTypes != null)
      require(paramTypes.length == nParams, s"paramTypes length is invalid, expected $nParams but is ${paramTypes.length}")

    val paramFormats = if (nParams == 0) null else Array.fill[Int](nParams)(1)
    val paramLengths = paramValues.map(_.length).toArray

    Zone { implicit z =>
      val cCommand = toCString(command)
      val cParamTypes = if (nParams == 0 || paramTypes == null) null else oidSeq2oidPtr(paramTypes)
      val cParamValues = if (nParams == 0) null else byteArrays2cstrPtr(paramValues)
      val cParamLengths = if (nParams == 0) null else intArray2intPtr(paramLengths)
      val cParamFormats = if (nParams == 0) null else intArray2intPtr(paramFormats)

      val querySent = pq.PQsendQueryParams(conn, cCommand, nParams, cParamTypes, cParamValues, cParamLengths, cParamFormats, resultFormat = 1)

      if (querySent == 0) false else true
    }
  }

  //def PQsendPrepare(conn: IPGconn, stmtName: String, query: String, nParams: Int, paramTypes: IOid): Int

  //def PQsendQueryPrepared(conn: IPGconn, stmtName: String, nParams: Int, paramValues: Seq[String], paramLengths: Array[Int], paramFormats: Array[Int], resultFormat: Int): Int

  @inline def PQsetSingleRowMode(conn: IPGconn): Int = {
    require(conn != null, "conn is null")
    pq.PQsetSingleRowMode(conn)
  }

  @inline def PQgetResult(conn: IPGconn): IPGresult = {
    require(conn != null, "conn is null")
    pq.PQgetResult(conn)
  }

  @inline def PQisBusy(conn: IPGconn): Boolean = {
    require(conn != null, "conn is null")
    if (pq.PQisBusy(conn) == 0) false else true
  }

  @inline def PQconsumeInput(conn: IPGconn): Boolean = {
    require(conn != null, "conn is null")
    if (pq.PQconsumeInput(conn) == 0) false else true
  }

  //def PQnotifies(conn: IPGconn): Ptr[PGnotify]

  def PQputCopyData(conn: IPGconn, buffer: Array[Byte], offset: Int = 0, size: Int = 0): util.Try[Boolean] = {
    require(conn != null, "conn is null")
    require(buffer != null, "buffer is null")
    require(offset >= 0, "invalid offset value")
    require(size >= 0, "invalid size value")

    val bufferPtr = buffer.asInstanceOf[scala.scalanative.runtime.ByteArray].at(offset)
    val bufferLen = if (size == 0) buffer.length else size
    val copyDataRC = pq.PQputCopyData(conn, bufferPtr, bufferLen)

    if (copyDataRC == 1) util.Success(true)
    else if (copyDataRC == 0) util.Success(false)
    else if (copyDataRC == -1) util.Failure(new Exception(s"an error occurred"))
    else throw new Exception("invalid PQputCopyData return code")
  }

  def PQputCopyEnd(conn: IPGconn, errorMsg: Option[String]): util.Try[Boolean] = {
    require(conn != null, "conn is null")

    Zone { implicit z =>
      val msg = errorMsg.orNull
      val cErrMsg = if (msg == null) null else toCString(msg)

      val copyEndRC = pq.PQputCopyEnd(conn, cErrMsg)

      if (copyEndRC == 1) util.Success(true)
      else if (copyEndRC == 0) util.Success(false)
      else if (copyEndRC == -1) util.Failure(new Exception(s"an error occurred: $errorMsg"))
      else throw new Exception("invalid PQputCopyEnd return code")
    }
  }

  def PQgetCopyData(conn: IPGconn, async: Boolean): util.Either[Array[Byte], Int] = {
    require(conn != null, "conn is null")

    val bufferPtr = stackalloc[Ptr[Byte]]
    val copyDataRC = pq.PQgetCopyData(
      conn,
      bufferPtr,
      if (async) 1 else 0
    )

    if (copyDataRC <= 0) util.Right(copyDataRC)
    else {
      val buffer = !bufferPtr
      assert(buffer != null, s"the buffer should be null since we have read $copyDataRC bytes")
      val rowAsBytes = util.Left(CUtils.bytes2ByteArray(buffer, copyDataRC))
      pq.PQfreemem(buffer)
      rowAsBytes
    }
  }

  //@deprecated def PQgetline(conn: IPGconn, string: String, length: Int): Int
  //@deprecated def PQputline(conn: IPGconn, string: String): Int
  //@deprecated def PQgetlineAsync(conn: IPGconn, buffer: String, bufsize: Int): Int
  //@deprecated def PQputnbytes(conn: IPGconn, buffer: String, nbytes: Int): Int
  //@deprecated def PQendcopy(conn: IPGconn): Int

  @inline def PQsetnonblocking(conn: IPGconn, setNonBlocking: Boolean): Boolean = {
    require(conn != null, "conn is null")
    val rc = pq.PQsetnonblocking(conn, if (setNonBlocking) 1 else 0)
    if (rc == 0) true else false
  }

  @inline def PQisnonblocking(conn: IPGconn): Boolean = {
    require(conn != null, "conn is null")
    if (pq.PQisnonblocking(conn) == 0) false else true
  }

  @inline def PQisthreadsafe(): Boolean = {
    if (pq.PQisthreadsafe() == 0) false else true
  }

  //def PQregisterThreadLock(newhandler: CFuncPtr1[Int, Unit]): CFuncPtr1[Int, Unit]

  def PQping(conninfo: String): IPGPing = {
    require(conninfo != null, "conninfo is null")

    Zone { implicit z =>
      val pingRC = pq.PQping(toCString(conninfo))

      PGPing
        .withValue(pingRC.toInt)
        .getOrElse(throw new Exception(s"invalid PGPing value: $pingRC"))
    }
  }

  def PQpingParams(params: Seq[(String,String)], expandDbName: Boolean): IPGPing = {
    require(params != null, "params is null")

    val keywords = params.map(_._1)
    val values = params.map(_._2)

    Zone { implicit z =>
      val cKeywords = strs2cstrPtr(keywords, appendNullStr = true)
      val cValues = strs2cstrPtr(values, appendNullStr = true)

      val pingRC = pq.PQpingParams(cKeywords, cValues, if (expandDbName) 1 else 0)

      PGPing
        .withValue(pingRC.toInt)
        .getOrElse(throw new Exception(s"invalid PGPing value: $pingRC"))
    }
  }

  @inline def PQflush(conn: IPGconn): Int = {
    require(conn != null, "conn is null")
    pq.PQflush(conn)
  }

  //def PQfn(conn: IPGconn, fnid: Int, result_buf: Array[Int], result_len: Array[Int], result_is_int: Int, args: Ptr[PQArgBlock], nargs: Int): IPGresult

  def PQresultStatus(res: IPGresult): IExecStatusType = {
    require(res != null, "res is null")

    val statusTypeCode = pq.PQresultStatus(res).toInt
    ExecStatusType
      .withCode(statusTypeCode)
      .getOrElse(throw new Exception(s"invalid ExecStatusType code: $statusTypeCode"))
  }

  def PQresStatus(status: IExecStatusType): String = {
    require(status != null, "status is null")

    val execStatusName = pq.PQresStatus(status.getCode().toUInt)
    if (execStatusName == null) null else fromCString(execStatusName)
  }

  def PQresultErrorMessage(res: IPGresult): String = {
    require(res != null, "res is null")

    val cErrMsg = pq.PQresultErrorMessage(res)
    if (cErrMsg == null) "" else fromCString(cErrMsg)
  }

  //def PQresultVerboseErrorMessage(res: IPGresult, verbosity: PGVerbosity, show_context: PGContextVisibility): String

  def PQresultErrorField(res: IPGresult, fieldIdentifier: IPGErrorFieldIdentifier): String = {
    require(res != null, "res is null")

    val cErrMsg = pq.PQresultErrorField(res, fieldIdentifier.getValue())
    if (cErrMsg == null) null else fromCString(cErrMsg)
  }

  @inline def PQntuples(res: IPGresult): Int = {
    require(res != null, "res is null")
    pq.PQntuples(res)
  }

  @inline def PQnfields(res: IPGresult): Int = {
    require(res != null, "res is null")
    pq.PQnfields(res)
  }

  @inline def PQbinaryTuples(res: IPGresult): Boolean = {
    require(res != null, "res is null")
    if (pq.PQbinaryTuples(res) == 0) false else true
  }

  @inline def PQfname(res: IPGresult, fieldNum: Int): String = {
    require(res != null, "res is null")

    val fieldName = pq.PQfname(res, fieldNum)
    if (fieldName == null) null else fromCString(fieldName)
  }

  @inline def PQfnumber(res: IPGresult, fieldName: String): Int = {
    require(res != null, "res is null")
    require(fieldName != null, "fieldName is null")

    Zone { implicit z =>
      pq.PQfnumber(res, toCString(fieldName))
    }
  }

  @inline def PQftable(res: IPGresult, fieldNum: Int): IOidBox = {
    require(res != null, "res is null")
    pq.PQftable(res, fieldNum)
  }

  @inline def PQftablecol(res: IPGresult, fieldNum: Int): Int = {
    require(res != null, "res is null")
    pq.PQftablecol(res, fieldNum)
  }

  @inline def PQfformat(res: IPGresult, fieldNum: Int): Int = {
    require(res != null, "res is null")
    pq.PQfformat(res, fieldNum)
  }

  @inline def PQftype(res: IPGresult, fieldNum: Int): IOidBox = {
    require(res != null, "res is null")
    pq.PQftype(res, fieldNum)
  }

  @inline def PQfsize(res: IPGresult, fieldNum: Int): Int = {
    require(res != null, "res is null")
    pq.PQfsize(res, fieldNum)
  }

  @inline def PQfmod(res: IPGresult, fieldNum: Int): Int = {
    require(res != null, "res is null")
    pq.PQfmod(res, fieldNum)
  }

  @inline def PQcmdStatus(res: IPGresult): String = {
    require(res != null, "res is null")

    val cmdStatus = pq.PQcmdStatus(res)
    if (cmdStatus == null) null else fromCString(cmdStatus)
  }

  /*@deprecated def PQoidStatus(res: IPGresult): String = {
    require(res != null, "res is null")

    val fieldName = pq.PQoidStatus(res, fieldNum)
    if (fieldName == null) null else fromCString(fieldName)
  }*/

  @inline def PQoidValue(res: IPGresult): IOidBox = {
    require(res != null, "res is null")
    pq.PQoidValue(res)
  }

  @inline def PQcmdTuples(res: IPGresult): Int = {
    require(res != null, "res is null")
    val cAffectedRows = pq.PQcmdTuples(res)
    if (cAffectedRows == null) 0
    else {
      val affectedRows = fromCString(cAffectedRows)
      if (affectedRows.isEmpty) 0 else affectedRows.toInt
    }
  }

  @inline def PQgetvalue(res: IPGresult, tupNum: Int, fieldNum: Int): String = {
    require(res != null, "res is null")
    val fieldValue = pq.PQgetvalue(res, tupNum, fieldNum)
    if (fieldValue == null) null else fromCString(fieldValue)
  }

  @inline def PQgetlength(res: IPGresult, tupNum: Int, fieldNum: Int): Int = {
    require(res != null, "res is null")
    pq.PQgetlength(res, tupNum, fieldNum)
  }

  @inline def PQgetisnull(res: IPGresult, tupNum: Int, fieldNum: Int): Boolean = {
    require(res != null, "res is null")
    val isNull = pq.PQgetisnull(res, tupNum, fieldNum)
    if (isNull == 0) false else true
  }

  @inline def PQnparams(res: IPGresult): Int = {
    require(res != null, "res is null")
    pq.PQnparams(res)
  }

  @inline def PQparamtype(res: IPGresult, paramNum: Int): IOidBox = {
    require(res != null, "res is null")
    pq.PQparamtype(res, paramNum)
  }

  /*def PQdescribePrepared(conn: IPGconn, stmt: String): IPGresult
  def PQdescribePortal(conn: IPGconn, portal: String): IPGresult
  def PQsendDescribePrepared(conn: IPGconn, stmt: String): Int
  def PQsendDescribePortal(conn: IPGconn, portal: String): Int
  def PQclear(res: IPGresult): Unit
  def PQfreemem(ptr: Ptr[Byte]): Unit
  def PQmakeEmptyPGresult(conn: IPGconn, status: IExecStatusType): IPGresult
  def PQcopyResult(src: IPGresult, flags: Int): IPGresult
  def PQsetResultAttrs(res: IPGresult, numAttributes: Int, attDescs: Ptr[PGresAttDesc]): Int
  def PQresultAlloc(res: IPGresult, nBytes: CSize): Ptr[Byte]
  def PQsetvalue(res: IPGresult, tup_num: Int, field_num: Int, value: String, len: Int): Int
  def PQescapeStringConn(conn: IPGconn, to: String, from: String, length: CSize, error: Ptr[Int]): CSize
  def PQescapeLiteral(conn: IPGconn, str: String, len: CSize): String
  def PQescapeIdentifier(conn: IPGconn, str: String, len: CSize): String
  def PQescapeByteaConn(conn: IPGconn, from: Ptr[CUnsignedChar], from_length: CSize, to_length: Ptr[CSize]): Ptr[CUnsignedChar]
  def PQunescapeBytea(strtext: Ptr[CUnsignedChar], retbuflen: Ptr[CSize]): Ptr[CUnsignedChar]
  def PQescapeString(to: String, from: String, length: CSize): CSize
  def PQescapeBytea(from: Ptr[CUnsignedChar], from_length: CSize, to_length: Ptr[CSize]): Ptr[CUnsignedChar]
  def PQprint(fout: Ptr[FILE], res: IPGresult, ps: Ptr[PQprintOpt]): Unit
  def PQdisplayTuples(res: IPGresult, fp: Ptr[FILE], fillAlign: Int, fieldSep: String, printHeader: Int, quiet: Int): Unit
  def PQprintTuples(res: IPGresult, fout: Ptr[FILE], printAttName: Int, terseOutput: Int, width: Int): Unit*/



  /*def lo_open(conn: IPGconn, lobjId: Oid, mode: Int): Int
  def lo_close(conn: IPGconn, fd: Int): Int
  def lo_read(conn: IPGconn, fd: Int, buf: String, len: CSize): Int
  def lo_write(conn: IPGconn, fd: Int, buf: String, len: CSize): Int
  def lo_lseek(conn: IPGconn, fd: Int, offset: Int, whence: Int): Int
  def lo_lseek64(conn: IPGconn, fd: Int, offset: pg_int64, whence: Int): pg_int64
  def lo_creat(conn: IPGconn, mode: Int): Oid
  def lo_create(conn: IPGconn, lobjId: Oid): Oid
  def lo_tell(conn: IPGconn, fd: Int): Int
  def lo_tell64(conn: IPGconn, fd: Int): pg_int64
  def lo_truncate(conn: IPGconn, fd: Int, len: CSize): Int
  def lo_truncate64(conn: IPGconn, fd: Int, len: pg_int64): Int
  def lo_unlink(conn: IPGconn, lobjId: Oid): Int
  def lo_import(conn: IPGconn, filename: String): Oid
  def lo_import_with_oid(conn: IPGconn, filename: String, lobjId: Oid): Oid
  def lo_export(conn: IPGconn, lobjId: Oid, filename: String): Int*/


  /*def PQmblen(s: String, encoding: Int): Int
  def PQdsplen(s: String, encoding: Int): Int
  def PQenv2encoding(): Int
  def PQencryptPassword(passwd: String, user: String): String
  def PQencryptPasswordConn(conn: IPGconn, passwd: String, user: String, algorithm: String): String */

  def pg_char_to_encoding(name: String): Int = {
    require(name != null, "name is null")

    Zone { implicit z =>
      pq.pg_char_to_encoding(toCString(name))
    }
  }

  @inline def pg_encoding_to_char(encoding: Int): String = {
    val cstr = pq.pg_encoding_to_char(encoding)
    if (cstr == null) null else fromCString(cstr)
  }

  //def pg_valid_server_encoding_id(encoding: Int): Int

}
