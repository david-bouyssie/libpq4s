package com.github.libpq4s.library

import scala.util._

import com.github.libpq4s.api._

// Extra types
trait IFile extends Any
trait IOidBox extends Any { def toInt(): Int }
trait IPGcancel extends Any // TODO: rename IPGcancelWrapper ?
trait IPGconn extends Any
trait IPGresult extends Any

trait ILibPQ {

  /**
   * PQconnectStart
   *
   * Begins the establishment of a connection to a postgres backend through the
   * postmaster using connection information in a string.
   *
   * See comment for PQconnectdb for the definition of the string format.
   *
   * Returns a PGconn*. If NULL is returned, a malloc error has occurred, and you
   * should not attempt to proceed with this connection. If the status field of the
   * connection returned is CONNECTION_BAD, an error has occurred. In this case you
   * should call PQfinish on the result, (perhaps inspecting the error message
   * first). Other fields of the structure may not be valid if that occurs. If the
   * status field is not CONNECTION_BAD, then this stage has succeeded - call
   * PQconnectPoll, using select(2) to see when this is necessary.
   *
   * See PQconnectPoll for more info.
   *
   * Allocate memory for the conn structure
   *
   * Parse the conninfo string
   *
   * Compute derived options
   *
   * Connect to the database
   *
   * Just in case we failed to set it in connectDBStart
   */
  def PQconnectStart(conninfo: String): IPGconn

  /**
   * PQconnectStartParams
   *
   * Begins the establishment of a connection to a postgres backend through the
   * postmaster using connection information in a struct.
   *
   * See comment for PQconnectdbParams for the definition of the string format.
   *
   * Returns a PGconn*. If NULL is returned, a malloc error has occurred, and you
   * should not attempt to proceed with this connection. If the status field of the
   * connection returned is CONNECTION_BAD, an error has occurred. In this case you
   * should call PQfinish on the result, (perhaps inspecting the error message
   * first). Other fields of the structure may not be valid if that occurs. If the
   * status field is not CONNECTION_BAD, then this stage has succeeded - call
   * PQconnectPoll, using select(2) to see when this is necessary.
   *
   * See PQconnectPoll for more info.
   *
   * Allocate memory for the conn structure
   *
   * Parse the conninfo arrays
   *
   * errorMessage is already set
   *
   * Move option values into conn structure
   *
   * Free the option info - all is in conn now
   *
   * Compute derived options
   *
   * Connect to the database
   *
   * Just in case we failed to set it in connectDBStart
   */
  def PQconnectStartParams(params: collection.Seq[(String,String)], expandDbName: Boolean): IPGconn

  /**
   *  PQconnectPoll
   *
   * Poll an asynchronous connection.
   *
   * Returns a PostgresPollingStatusType. Before calling this function, use select(2)
   * to determine when data has arrived..
   *
   * You must call PQfinish whether or not this fails.
   *
   * This function and PQconnectStart are intended to allow connections to be made
   * without blocking the execution of your program on remote I/O. However, there are
   * a number of caveats:
   *
   * o If you call PQtrace, ensure that the stream object into which you trace will
   * not block. o If you do not supply an IP address for the remote host (i.e. you
   * supply a host name instead) then PQconnectStart will block on gethostbyname. You
   * will be fine if using Unix sockets (i.e. by supplying neither a host name nor a
   * host address). o If your backend wants to use Kerberos authentication then you
   * must supply both a host name and a host address, otherwise this function may
   * block on gethostname.
   *
   */
  def PQconnectPoll(conn: IPGconn): IPostgresPollingStatusType

  /**
   * PQconnectdb
   *
   * establishes a connection to a postgres backend through the postmaster using
   * connection information in a string.
   *
   * The conninfo string is either a whitespace-separated list of
   *
   *     option = value
   *
   * definitions or a URI (refer to the documentation for details.) Value might be a
   * single value containing no whitespaces or a single quoted string. If a single
   * quote should appear anywhere in the value, it must be escaped with a backslash
   * like \\'
   *
   * Returns a PGconn* which is needed for all subsequent libpq calls, or NULL if a
   * memory allocation failed. If the status field of the connection returned is
   * CONNECTION_BAD, then some fields may be null'ed out instead of having valid
   * values.
   *
   * You should call PQfinish (if conn is not NULL) regardless of whether this call
   * succeeded.
   */
  def PQconnectdb(conninfo: String): IPGconn

  /**
   * Connecting to a Database
   *
   * There are now six different ways a user of this API can connect to the database.
   * Two are not recommended for use in new code, because of their lack of
   * extensibility with respect to the passing of options to the backend. These are
   * PQsetdb and PQsetdbLogin (the former now being a macro to the latter).
   *
   * If it is desired to connect in a synchronous (blocking) manner, use the function
   * PQconnectdb or PQconnectdbParams. The former accepts a string of option = value
   * pairs (or a URI) which must be parsed; the latter takes two NULL terminated
   * arrays instead.
   *
   * To connect in an asynchronous (non-blocking) manner, use the functions
   * PQconnectStart or PQconnectStartParams (which differ in the same way as
   * PQconnectdb and PQconnectdbParams) and PQconnectPoll.
   *
   * Internally, the static functions connectDBStart, connectDBComplete are part of
   * the connection procedure. PQconnectdbParams
   *
   * establishes a connection to a postgres backend through the postmaster using
   * connection information in two arrays.
   *
   * The keywords array is defined as
   *
   *     const char *params[] = {\"option1\", \"option2\", NULL}
   *
   * The values array is defined as
   *
   *     const char *values[] = {\"value1\", \"value2\", NULL}
   *
   * Returns a PGconn* which is needed for all subsequent libpq calls, or NULL if a
   * memory allocation failed. If the status field of the connection returned is
   * CONNECTION_BAD, then some fields may be null'ed out instead of having valid
   * values.
   *
   * You should call PQfinish (if conn is not NULL) regardless of whether this call
   * succeeded.
   */
  def PQconnectdbParams(params: collection.Seq[(String,String)], expandDbName: Boolean): IPGconn

  /**
   *  PQsetdbLogin: Makes a new connection to the database server.
   *
   * This is the predecessor of PQconnectdb with a fixed set of parameters.
   * It has the same functionality except that the missing parameters will always take on default values.
   * Write NULL or an empty string for any one of the fixed parameters that is to be defaulted.
   *
   * If the dbName contains an = sign or has a valid connection URI prefix,
   * it is taken as a conninfo string in exactly the same way as if it had been passed to PQconnectdb,
   * and the remaining parameters are then applied as specified for PQconnectdbParams.
   */
  @deprecated("use PQconnectdb instead","0.0.1") def PQsetdbLogin(pghost: String, pgport: String, pgoptions: String, pgtty: String, dbName: String, login: String, pwd: String): IPGconn

  /**
   * PQfinish: properly close a connection to the backend. Also frees the PGconn data
   * structure so it shouldn't be re-used after this.
   */
  def PQfinish(conn: IPGconn): Unit

  /**
   * Returns the default connection options.
   *
   * Returns a connection options array. This can be used to determine all possible PQconnectdb options and their current default values.
   * The return value points to an array of PQconninfoOption structures, which ends with an entry having a null keyword pointer.
   * The null pointer is returned if memory could not be allocated.
   * Note that the current default values (val fields) will depend on environment variables and other context.
   * A missing or invalid service file will be silently ignored. Callers must treat the connection options data as read-only.
   *
   * After processing the options array, free it by passing it to PQconninfoFree.
   * If this is not done, a small amount of memory is leaked for each call to PQconndefaults.
   */
  def PQconndefaults(): IPQconninfoOptionWrapper

  /**
   * PQconninfoParse
   *
   * Returns parsed connection options from the provided connection string.
   *
   * Parses a connection string like PQconnectdb() and returns the resulting options as an array;
   * or returns NULL if there is a problem with the connection string.
   * This function can be used to extract the PQconnectdb options in the provided connection string (not any possible default values).
   * The return value points to an array of PQconninfoOption structures, which ends with an entry having a null keyword pointer.
   *
   * All legal options will be present in the result array, but the PQconninfoOption for any option not present in the connection string
   * will have val set to NULL; default values are not inserted.
   *
   * After processing the options array (which is dynamically allocated), free it by passing it to PQconninfoFree.
   * If this is not done, some memory is leaked for each call to PQconninfoParse.
   *
   */
  def PQconninfoParse(conninfo: String): Try[IPQconninfoOptionWrapper]

  /**
   * Returns the connection options used by a live connection.
   *
   * Returns a connection options array.
   * This can be used to determine all possible PQconnectdb options and the values that were used to connect to the server.
   * The return value points to an array of PQconninfoOption structures, which ends with an entry having a null keyword pointer.
   * All notes above for PQconndefaults also apply to the result of PQconninfo.
   */
  def PQconninfo(conn: IPGconn): IPQconninfoOptionWrapper

  def PQconninfoFree(connOptions: IPQconninfoOptionWrapper): Unit

  /**
   * PQresetStart: resets the connection to the backend closes the existing
   * connection and makes a new one Returns 1 on success, 0 on failure.
   */
  //def PQresetStart(conn: IPGconn): Int

  /**
   * PQresetPoll: resets the connection to the backend closes the existing connection
   * and makes a new one
   *
   * Notify event procs of successful reset. We treat an event proc failure as
   * disabling the connection ... good idea?
   */
  //def PQresetPoll(conn: IPGconn): IPostgresPollingStatusType

  /**
   * PQreset: Resets the communication channel to the server.
   *
   * This function will close the connection to the server and attempt to reestablish a new connection to the same server,
   * using all the same parameters previously used. This might be useful for error recovery if a working connection is lost.
   */
  def PQreset(conn: IPGconn): Unit

  /**
   * PQgetCancel: Creates a data structure containing the information needed to cancel a command issued through a particular database connection.
   *
   * PQgetCancel creates a PGcancel object given a PGconn connection object.
   * It will return NULL if the given conn is NULL or an invalid connection.
   * The PGcancel object is an opaque structure that is not meant to be accessed directly by the application;
   * it can only be passed to PQcancel or PQfreeCancel.
   *
   * A copy is needed to be able to cancel a running query from a different thread.
   * If the same structure is used all structure members would have to be
   * individually locked (if the entire structure was locked, it would be impossible
   * to cancel a synchronous query because the structure would have to stay locked for the duration of the query).
   */
  def PQgetCancel(conn: IPGconn): IPGcancel

  /**
   * PQfreeCancel: free a cancel structure
   */
  def PQfreeCancel(cancel: IPGcancel): Unit

  /**
   * PQcancel: request query cancel
   *
   * Returns true if able to send the cancel request, false if not.
   *
   * On failure, an error message is stored in *errbuf, which must be of size
   * errbufsize (recommended size is 256 bytes). *errbuf is not changed on success
   * return.
   */
  //def PQcancel(cancel: IPGcancel, errbuf: String, errbufsize: Int): Int
  def PQcancel(cancel: IPGcancel, errbufsize: Int = 512): Try[Unit]

  /**
   * PQrequestCancel: old, not thread-safe function for requesting query cancel
   *
   * Returns true if able to send the cancel request, false if not.
   *
   * On failure, the error message is saved in conn->errorMessage; this means that
   * this can't be used when there might be other active operations on the connection
   * object.
   *
   */
  //@deprecated def PQrequestCancel(conn: IPGconn): Int

  /*
   * The following functions return parameter values established at connection.
   * These values are fixed for the life of the PGconn object.
   */

  /** Returns the database name of the connection. */
  def PQdb(conn: IPGconn): String
  /** Returns the user name of the connection. */
  def PQuser(conn: IPGconn): String
  /** Returns the password of the connection. */
  def PQpass(conn: IPGconn): String
  /** Returns the host name of the connection. */
  def PQhost(conn: IPGconn): String
  /** Returns the port of the connection. */
  def PQport(conn: IPGconn): String
  /* Returns the debug TTY of the connection (obsolete but kept for backwards compatibility). */
  //@deprecated def PQtty(conn: IPGconn): String
  /** Returns the command-line options passed in the connection request. */
  def PQoptions(conn: IPGconn): String

  /*
   * The following functions return status data that can change as operations are executed on the PGconn object.
   */

  /**
   * Returns the status of the connection.
   *
   * The status can be one of a number of values.
   * However, only two of these are seen outside of an asynchronous connection procedure: CONNECTION_OK and CONNECTION_BAD.
   * A good connection to the database has the status CONNECTION_OK.
   * A failed connection attempt is signaled by status CONNECTION_BAD.
   * Ordinarily, an OK status will remain so until PQfinish,
   * but a communications failure might result in the status changing to CONNECTION_BAD prematurely.
   * In that case the application could try to recover by calling PQreset.
   *
   * See the entry for PQconnectStart and PQconnectPoll with regards to other status codes that might be seen.
   */
  def PQstatus(conn: IPGconn): IConnStatusType

  /**
   * Returns the current in-transaction status of the server.
   *
   * The status can be PQTRANS_IDLE (currently idle), PQTRANS_ACTIVE (a command is in progress),
   * PQTRANS_INTRANS (idle, in a valid transaction block), or PQTRANS_INERROR (idle, in a failed transaction block).
   *
   * PQTRANS_UNKNOWN is reported if the connection is bad.
   * PQTRANS_ACTIVE is reported only when a query has been sent to the server and not yet completed.
   */
  def PQtransactionStatus(conn: IPGconn): ITransactionStatusType

  /**
   * Looks up a current parameter setting of the server.
   *
   * Certain parameter values are reported by the server automatically at connection startup or whenever their values change.
   * PQparameterStatus can be used to interrogate these settings.
   * It returns the current value of a parameter if known, or NULL if the parameter is not known.
   *
   * Parameters reported as of the current release include server_version, server_encoding, client_encoding, application_name,
   * is_superuser, session_authorization, DateStyle, IntervalStyle, TimeZone, integer_datetimes, and standard_conforming_strings.
   * Sserver_encoding, TimeZone, and integer_datetimes were not reported by releases before 8.0;
   * standard_conforming_strings was not reported by releases before 8.1;
   * IntervalStyle was not reported by releases before 8.4;
   * application_name was not reported by releases before 9.0.
   * Note that server_version, server_encoding and integer_datetimes cannot change after startup.
   *
   * Pre-3.0-protocol servers do not report parameter settings, but libpq includes logic to obtain values for server_version and client_encoding anyway.
   * Applications are encouraged to use PQparameterStatus rather than ad hoc code to determine these values.
   * (Beware however that on a pre-3.0 connection, changing client_encoding via SET after connection startup will not be reflected by PQparameterStatus.)
   * For server_version, see also PQserverVersion, which returns the information in a numeric form that is much easier to compare against.
   *
   * If no value for standard_conforming_strings is reported, applications can assume it is off, that is, backslashes are treated as escapes in string literals.
   * Also, the presence of this parameter can be taken as an indication that the escape string syntax (E'...') is accepted.
   *
   * Although the returned pointer is declared const, it in fact points to mutable storage associated with the PGconn structure.
   * It is unwise to assume the pointer will remain valid across queries.
   */
  def PQparameterStatus(conn: IPGconn, paramName: String): String

  def PQlibVersion(): Int

  /**
   * Interrogates the frontend/backend protocol being used.
   *
   * Applications might wish to use this function to determine whether certain features are supported.
   * Currently, the possible values are 2 (2.0 protocol), 3 (3.0 protocol), or zero (connection bad).
   * The protocol version will not change after connection startup is complete, but it could theoretically change during a connection reset.
   * The 3.0 protocol will normally be used when communicating with PostgreSQL 7.4 or later servers; pre-7.4 servers support only protocol 2.0.
   * (Protocol 1.0 is obsolete and not supported by libpq.)
   */
  def PQprotocolVersion(conn: IPGconn): Int

  /**
   * Returns an integer representing the server version.
   *
   * Applications might use this function to determine the version of the database server they are connected to.
   * The result is formed by multiplying the server's major version number by 10000 and adding the minor version number.
   * For example, version 10.1 will be returned as 100001, and version 11.0 will be returned as 110000.
   * Zero is returned if the connection is bad.
   *
   * Prior to major version 10, PostgreSQL used three-part version numbers in which the first two parts together represented the major version.
   * For those versions, PQserverVersion uses two digits for each part;
   * for example version 9.1.5 will be returned as 90105, and version 9.2.0 will be returned as 90200.
   *
   * Therefore, for purposes of determining feature compatibility,
   * applications should divide the result of PQserverVersion by 100 not 10000 to determine a logical major version number.
   * In all release series, only the last two digits differ between minor releases (bug-fix releases).
   */
  def PQserverVersion(conn: IPGconn): Int

  /**
   * Returns the error message most recently generated by an operation on the connection.
   *
   * Nearly all libpq functions will set a message for PQerrorMessage if they fail.
   * Note that by libpq convention, a nonempty PQerrorMessage result can consist of multiple lines, and will include a trailing newline.
   * The caller should not free the result directly. It will be freed when the associated PGconn handle is passed to PQfinish.
   * The result string should not be expected to remain the same across operations on the PGconn structure.
   *
   */
  def PQerrorMessage(conn: IPGconn): String

  /**
   * Obtains the file descriptor number of the connection socket to the server.
   *
   * A valid descriptor will be greater than or equal to 0; a result of -1 indicates that no server connection is currently open.
   * This will not change during normal operation, but could change during connection setup or reset.
   *
   * In Windows, socket values are unsigned, and an invalid socket value
   * (INVALID_SOCKET) is ~0, which equals -1 in comparisons (with no compiler
   * warning). Ideally we would return an unsigned value for PQsocket() on Windows,
   * but that would cause the function's return value to differ from Unix, so we just
   * return -1 for invalid sockets. http://msdn.microsoft.com/en-
   * us/library/windows/desktop/cc507522%28v=vs.85%29.aspx
   * http://stackoverflow.com/questions/10817252/why-is-invalid-socket-defined-
   * as-0-in-winsock2-h-c
   */
  def PQsocket(conn: IPGconn): Int

  /**
   * Returns the process ID (PID) of the backend process handling this connection.
   *
   * The backend PID is useful for debugging purposes and for comparison to NOTIFY messages (which include the PID of the notifying backend process).
   * Note that the PID belongs to a process executing on the database server host, not the local host!
   */
  def PQbackendPID(conn: IPGconn): Int

  /**
   * Returns true (1) if the connection authentication method required a password, but none was available. Returns false (0) if not.
   *
   * This function can be applied after a failed connection attempt to decide whether to prompt the user for a password.
   */
  def PQconnectionNeedsPassword(conn: IPGconn): Boolean

  /**
   * Returns true (1) if the connection authentication method used a password. Returns false (0) if not.
   *
   * This function can be applied after either a failed or successful connection attempt to detect whether the server demanded a password.
   */
  def PQconnectionUsedPassword(conn: IPGconn): Boolean

  /**
   * Returns the client encoding.
   *
   * ote that it returns the encoding ID, not a symbolic string such as EUC_JP.
   * To convert an encoding ID to an encoding name, you can call pg_encoding_to_char().
   */
  def PQclientEncoding(conn: IPGconn): Int

  /**
   * Sets the client encoding.
   *
   * The current encoding for this connection can be determined by using PQclientEncoding.
   *
   * @param conn a connection to the server
   * @param encoding the encoding you want to use
   * @return true if the function successfully sets the encoding, otherwise false
   */
  def PQsetClientEncoding(conn: IPGconn, encoding: String): Boolean

  /**
   * ENABLE_THREAD_SAFETY WIN32
   * ------------------------------------------------------------ Procedures common
   * to all secure sessions
   * ------------------------------------------------------------
   */
  //def PQsslInUse(conn: IPGconn): Int
  //def PQsslStruct(conn: IPGconn, struct_name: String): Ptr[Byte]

  /**
   * unknown attribute
   */
  //def PQsslAttribute(conn: IPGconn, attribute_name: String): String
  //def PQsslAttributeNames(conn: IPGconn): collection.Seq[String]

  /**
   * Dummy versions of SSL info functions, when built without SSL support
   */
  //def PQgetssl(conn: IPGconn): Ptr[Byte]

  /**
   * Exported function to allow application to tell us it's already initialized
   * OpenSSL.
   */
  //def PQinitSSL(do_init: Int): Unit

  /**
   * Exported function to allow application to tell us it's already initialized
   * OpenSSL and/or libcrypto.
   */
  //def PQinitOpenSSL(do_ssl: Int, do_crypto: Int): Unit

  /**
   * Determines the verbosity of messages returned by PQerrorMessage and PQresultErrorMessage.
   *
   * PQsetErrorVerbosity sets the verbosity mode, returning the connection's previous setting.
   * In TERSE mode, returned messages include severity, primary text, and position only; this will normally fit on a single line.
   * The DEFAULT mode produces messages that include the above plus any detail, hint, or context fields (these might span multiple lines).
   * The VERBOSE mode includes all available fields.
   * The SQLSTATE mode includes only the error severity and the SQLSTATE error code, if one is available (if not, the output is like TERSE mode).
   *
   * Changing the verbosity setting does not affect the messages available from already-existing PGresult objects, only subsequently-created ones.
   * But see PQresultVerboseErrorMessage if you want to print a previous error with a different verbosity.
   *
   * @return the previous verbosity mode of reported errors
   */
  def PQsetErrorVerbosity(conn: IPGconn, verbosity: IPGVerbosity): IPGVerbosity

  /**
   * Determines the handling of CONTEXT fields in messages returned by PQerrorMessage and PQresultErrorMessage.
   *
   * PQsetErrorContextVisibility sets the context display mode, returning the connection's previous setting.
   * This mode controls whether the CONTEXT field is included in messages.
   * The NEVER mode never includes CONTEXT, while ALWAYS always includes it if available.
   * In ERRORS mode (the default), CONTEXT fields are included only in error messages, not in notices and warnings.
   * However, if the verbosity setting is TERSE or SQLSTATE, CONTEXT fields are omitted regardless of the context display mode.
   *
   * Changing this mode does not affect the messages available from already-existing PGresult objects, only subsequently-created ones.
   * But see PQresultVerboseErrorMessage if you want to print a previous error with a different display mode.
   *
   * @return the previous setting
   */
  def PQsetErrorContextVisibility(conn: IPGconn, showContext: IPGContextVisibility): IPGContextVisibility

  /**
   * Enables tracing of the client/server communication to a debugging file stream.
   *
   * Note: On Windows, if the libpq library and an application are compiled with different flags,
   * this function call will crash the application because the internal representation of the FILE pointers differ.
   * Specifically, multithreaded/single-threaded, release/debug, and static/dynamic flags
   * should be the same for the library and all applications using that library.
   */
  //def PQtrace(conn: IPGconn, debugPort: IFile): Unit

  /** Disables tracing started by PQtrace. */
  //def PQuntrace(conn: IPGconn): Unit

  //def PQsetNoticeReceiver(conn: IPGconn, proc: CFuncPtr2[Ptr[Byte], IPGresult, Unit], arg: Ptr[Byte]): CFuncPtr2[Ptr[Byte], IPGresult, Unit]
  //def PQsetNoticeProcessor(conn: IPGconn, proc: CFuncPtr2[Ptr[Byte], String, Unit], arg: Ptr[Byte]): CFuncPtr2[Ptr[Byte], String, Unit]

  /**
   * PQexec sends a query to the backend and package up the result in a PGresult.
   *
   * If the query was not even sent, return NULL; conn->errorMessage is set to a
   * relevant message. If the query was sent, a new PGresult is returned (which could
   * indicate either success or failure). The user is responsible for freeing the
   * PGresult via PQclear() when done with it.
   */
  def PQexec(conn: IPGconn, query: String): IPGresult

  /*
  /**
   * PQexecParams is like PQexec, but use protocol 3.0 so we can pass parameters.
   *
   * It submits a command to the server and waits for the result, with the ability to pass parameters separately from the SQL command text.
   * The primary advantage of PQexecParams over PQexec is that parameter values can be separated from the command string, thus avoiding the need for tedious and error-prone quoting and escaping.
   * Unlike PQexec, PQexecParams allows at most one SQL command in the given string (there can be semicolons in it, but not more than one nonempty command).
   * This is a limitation of the underlying protocol, but has some usefulness as an extra defense against SQL-injection attacks.
   *
   * The function arguments are:
   *
   * @param conn The connection object to send the command through.
   * @param command The SQL command string to be executed. If parameters are used, they are referred to in the command string as $1, $2, etc.
   * @param nParams The number of parameters supplied; it is the length of the arrays paramTypes[], paramValues[], paramLengths[], and paramFormats[]. (The array pointers can be NULL when nParams is zero.)
   * @param paramTypes Specifies, by OID, the data types to be assigned to the parameter symbols. If paramTypes is NULL, or any particular element in the array is zero, the server infers a data type for the parameter symbol in the same way it would do for an untyped literal string.
   * @param paramValues Specifies the actual values of the parameters. A null pointer in this array means the corresponding parameter is null; otherwise the pointer points to a zero-terminated text string (for text format) or binary data in the format expected by the server (for binary format).
   * @param paramLengths Specifies the actual data lengths of binary-format parameters. It is ignored for null parameters and text-format parameters. The array pointer can be null when there are no binary parameters.
   * @param paramFormats Specifies whether parameters are text (put a zero in the array entry for the corresponding parameter) or binary (put a one in the array entry for the corresponding parameter). If the array pointer is null then all parameters are presumed to be text strings.
   * Values passed in binary format require knowledge of the internal representation expected by the backend. For example, integers must be passed in network byte order.
   * Passing numeric values requires knowledge of the server storage format, as implemented in src/backend/utils/adt/numeric.c::numeric_send() and src/backend/utils/adt/numeric.c::numeric_recv().
   * @param resultFormat Specify zero to obtain results in text format, or one to obtain results in binary format. (There is not currently a provision to obtain different result columns in different formats, although that is possible in the underlying protocol.)
   *
   */
  def PQexecParams(
    conn: IPGconn,
    command: String,
    nParams: Int,
    paramTypes: collection.Seq[IOidBox],
    paramValues: collection.Seq[String],
    paramLengths: Array[Int],
    paramFormats: Array[Int],
    resultFormat: Int
  ): IPGresult
  */

  /**
   * PQexecParamsBinary is like PQexec, but use protocol 3.0 so we can pass parameters.
   *
   * It submits a command to the server and waits for the result, with the ability to pass parameters separately from the SQL command text.
   * The primary advantage of PQexecParams over PQexec is that parameter values can be separated from the command string, thus avoiding the need for tedious and error-prone quoting and escaping.
   * Unlike PQexec, PQexecParams allows at most one SQL command in the given string (there can be semicolons in it, but not more than one nonempty command).
   * This is a limitation of the underlying protocol, but has some usefulness as an extra defense against SQL-injection attacks.
   *
   * The function arguments are:
   *
   * @param conn The connection object to send the command through.
   * @param command The SQL command string to be executed. If parameters are used, they are referred to in the command string as $1, $2, etc.
   * @param paramTypes Specifies, by OID, the data types to be assigned to the parameter symbols. If paramTypes is NULL, or any particular element in the array is zero, the server infers a data type for the parameter symbol in the same way it would do for an untyped literal string.
   * @param paramValues Specifies the actual values of the parameters. A null pointer in this array means the corresponding parameter is null; otherwise the pointer points to a zero-terminated text string (for text format) or binary data in the format expected by the server (for binary format).
   *
   */
  def PQexecParamsText(
    conn: IPGconn,
    command: String,
    paramValues: collection.Seq[String],
    paramTypes: collection.Seq[IOidBox] // TODO: Option?
  ): IPGresult

  /**
   * PQexecParamsBinary is like PQexec, but use protocol 3.0 so we can pass parameters.
   *
   * It submits a command to the server and waits for the result, with the ability to pass parameters separately from the SQL command text.
   * The primary advantage of PQexecParams over PQexec is that parameter values can be separated from the command string, thus avoiding the need for tedious and error-prone quoting and escaping.
   * Unlike PQexec, PQexecParams allows at most one SQL command in the given string (there can be semicolons in it, but not more than one nonempty command).
   * This is a limitation of the underlying protocol, but has some usefulness as an extra defense against SQL-injection attacks.
   *
   * The function arguments are:
   *
   * @param conn The connection object to send the command through.
   * @param command The SQL command string to be executed. If parameters are used, they are referred to in the command string as $1, $2, etc.
   * @param paramTypes Specifies, by OID, the data types to be assigned to the parameter symbols. If paramTypes is NULL, or any particular element in the array is zero, the server infers a data type for the parameter symbol in the same way it would do for an untyped literal string.
   * @param paramValues Specifies the actual values of the parameters. A null pointer in this array means the corresponding parameter is null; otherwise the pointer points to a zero-terminated text string (for text format) or binary data in the format expected by the server (for binary format).
   *
   */
  def PQexecParamsBinary(
    conn: IPGconn,
    command: String,
    paramValues: collection.Seq[Array[Byte]],
    paramTypes: collection.Seq[IOidBox] // TODO: Option?
  ): IPGresult

  /**
   * PQprepare Creates a prepared statement by issuing a v3.0 parse message.
   *
   * If the query was not even sent, return NULL; conn->errorMessage is set to a
   * relevant message. If the query was sent, a new PGresult is returned (which could
   * indicate either success or failure). The user is responsible for freeing the
   * PGresult via PQclear() when done with it.
   */
  def PQprepare(conn: IPGconn, stmtName: String, query: String, paramTypes: collection.Seq[IOidBox]): IPGresult  // TODO: paramTypes => Option?

  /**
   * PQexecPreparedText Like PQexec, but execute a previously prepared statement, using protocol 3.0 so we can pass parameters
   */
  def PQexecPreparedText(
    conn: IPGconn,
    stmtName: String,
    paramValues: collection.Seq[String]
  ): IPGresult

  /**
   * PQexecPreparedBinary Like PQexec, but execute a previously prepared statement, using protocol 3.0 so we can pass parameters
   */
  def PQexecPreparedBinary(
    conn: IPGconn,
    stmtName: String,
    paramValues: collection.Seq[Array[Byte]]
  ): IPGresult

  /**
   * PQsendQuery Submit a query, but don't wait for it to finish
   *
   * Returns: true if successfully submitted false if error (conn->errorMessage is set)
   *
   */
  def PQsendQuery(conn: IPGconn, query: String): Boolean

  /**
   * PQsendQueryParams Like PQsendQuery, but use protocol 3.0 so we can pass
   * parameters
   *
   * check the arguments
   *
   * use unnamed statement
   */
  /*def PQsendQueryParams(
    conn: IPGconn,
    command: String,
    nParams: Int,
    paramTypes: collection.Seq[IOidBox],
    paramValues: collection.Seq[String],
    paramLengths: Array[Int],
    paramFormats: Array[Int],
    resultFormat: Int
  ): Int*/
  def PQsendQueryParamsText(
    conn: IPGconn,
    command: String,
    paramValues: collection.Seq[String],
    paramTypes: collection.Seq[IOidBox] // TODO: Option?
  ): Boolean

  def PQsendQueryParamsBinary(
    conn: IPGconn,
    command: String,
    paramValues: collection.Seq[Array[Byte]],
    paramTypes: collection.Seq[IOidBox] // TODO: Option?
  ): Boolean

  /**
   * PQsendPrepare Submit a Parse message, but don't wait for it to finish
   *
   * Returns: 1 if successfully submitted 0 if error (conn->errorMessage is set)
   *
   * check the arguments
   *
   * This isn't gonna work on a 2.0 server
   *
   * construct the Parse message
   *
   * construct the Sync message
   *
   * remember we are doing just a Parse
   *
   * and remember the query text too, if possible
   *
   * if insufficient memory, last_query just winds up NULL
   *
   * Give the data a push. In nonblock mode, don't complain if we're unable to send
   * it all; PQgetResult() will do any additional flushing needed.
   *
   * OK, it's launched!
   *
   * error message should be set up already
   */
  //def PQsendPrepare(conn: IPGconn, stmtName: String, query: String, nParams: Int, paramTypes: IOid): Int

  /**
   * PQsendQueryPrepared Like PQsendQuery, but execute a previously prepared
   * statement, using protocol 3.0 so we can pass parameters
   *
   * check the arguments
   *
   * no command to parse
   *
   * no param types
   */
  //def PQsendQueryPrepared(conn: IPGconn, stmtName: String, nParams: Int, paramValues: collection.Seq[String], paramLengths: Array[Int], paramFormats: Array[Int], resultFormat: Int): Int

  /**
   * Select row-by-row processing mode
   *
   * Only allow setting the flag when we have launched a query and not yet received
   * any results.
   *
   * OK, set flag
   */
  def PQsetSingleRowMode(conn: IPGconn): Int

  /**
   * PQgetResult get the next PGresult produced by a query.
   *
   * Each non-null result from PQgetResult should be processed using the same PGresult accessor functions.
   * Returns NULL if no query work remains or an error has occurred (e.g. out of memory).
   * If called when no command is active, PQgetResult will just return a null pointer at once.
   *
   * Don't forget to free each result object with PQclear when done with it.
   * Note that PQgetResult will block only if a command is active and the necessary response data has not yet been read by PQconsumeInput.
   *
   */
  def PQgetResult(conn: IPGconn): IPGresult

  /**
   * PQisBusy Return true if PQgetResult would block waiting for input.
   *
   * Parse any available data, if our state permits.
   *
   * PQgetResult will return immediately in all states except BUSY, or if we had a
   * write failure.
   */
  def PQisBusy(conn: IPGconn): Boolean

  /**
   * Consume any available input from the backend.
   *
   * PQconsumeInput can be called even if the application is not prepared to deal with a result or notification just yet.
   * The function will read available data and save it in a buffer, thereby causing a select() read-ready indication to go away.
   * The application can thus use PQconsumeInput to clear the select() condition immediately, and then examine the results at leisure.
   *
   * PQconsumeInput normally returns true indicating “no error”, but returns false if there was some kind of trouble (in which case PQerrorMessage can be consulted).
   * Note that the result does not say whether any input data was actually collected.
   * After calling PQconsumeInput, the application can check PQisBusy and/or PQnotifies to see if their state has changed.
   *
   */
  def PQconsumeInput(conn: IPGconn): Boolean

  /**
   * PQnotifies returns a PGnotify* structure of the latest async notification that
   * has not yet been handled
   *
   * returns NULL, if there is currently no unhandled async notification from the
   * backend
   *
   * the CALLER is responsible for FREE'ing the structure returned
   *
   * Note that this function does not read any new data from the socket; so usually,
   * caller should call PQconsumeInput() first.
   *
   * Parse any available data to see if we can extract NOTIFY messages.
   *
   * don't let app see the internal state
   */
  //def PQnotifies(conn: IPGconn): Ptr[PGnotify]

  /**
   * Sends data to the server during COPY_IN state.
   *
   * Transmits the COPY data in the specified buffer, of length nbytes, to the server.
   * The result is 1 if the data was queued, zero if it was not queued because of full buffers (this will only happen in nonblocking mode),
   * or -1 if an error occurred. (Use PQerrorMessage to retrieve details if the return value is -1.
   * If the value is zero, wait for write-ready and try again.)
   *
   * The application can divide the COPY data stream into buffer loads of any convenient size.
   * Buffer-load boundaries have no semantic significance when sending.
   * The contents of the data stream must match the data format expected by the COPY command; see COPY for details.
   *
   * @return Success(true) if successful,
   *         Success(false) if data could not be sent (only possible in nonblocking mode),
   *         or Failure if an error occurs.
   */
  def PQputCopyData(conn: IPGconn, buffer: Array[Byte], offset: Int = 0, size: Int = 0): Try[Boolean]

  /**
   * Sends end-of-data indication to the server during COPY_IN state.
   *
   * Ends the COPY_IN operation successfully if errormsg is NULL.
   * If errormsg is not NULL then the COPY is forced to fail, with the string pointed to by errormsg used as the error message.
   * One should not assume that this exact error message will come back from the server, however,
   * as the server might have already failed the COPY for its own reasons.
   * Also note that the option to force failure does not work when using pre-3.0-protocol connections.
   *
   * The result is 1 if the termination message was sent;
   * or in nonblocking mode,this may only indicate that the termination message was successfully queued.
   *
   * In nonblocking mode, to be certain that the data has been sent,
   * you should next wait for write-ready and call PQflush, repeating until it returns zero.
   * Zero indicates that the function could not queue the termination message because of full buffers.
   * This will only happen in nonblocking mode (in this case, wait for write-ready and try the PQputCopyEnd call again).
   *
   * If a hard error occurs, -1 is returned; you can use PQerrorMessage to retrieve details.
   *
   * After successfully calling PQputCopyEnd, call PQgetResult to obtain the final result status of the COPY command.
   * One can wait for this result to be available in the usual way. Then return to normal operation.
   *
   * @return Success(true) if successful,
   *         Success(false) if data could not be sent (only possible in nonblocking mode),
   *         or Failure if an error occurs.
   */
  def PQputCopyEnd(conn: IPGconn, errorMsg: Option[String]): Try[Boolean]

  /**
   * Receives data from the server during COPY_OUT state.
   *
   * Attempts to obtain another row of data from the server during a COPY.
   * Data is always returned one data row at a time; if only a partial row is available, it is not returned.
   *
   * When a row is successfully returned, the return value is the number of data bytes in the row (this will always be greater than zero).
   * The returned string is always null-terminated, though this is probably only useful for textual COPY.
   *
   * A result of zero indicates that the COPY is still in progress, but no row is yet available (this is only possible when async is true).
   * When async is true, PQgetCopyData will not block waiting for input;
   * it will return zero if the COPY is still in progress but no complete row is available.
   * In this case wait for read-ready and then call PQconsumeInput before calling PQgetCopyData again.
   * When async is false, PQgetCopyData will block until data is available or the operation completes.
   *
   * A result of -1 indicates that the COPY is done.
   *
   * A result of -2 indicates that an error occurred (consult PQerrorMessage for the reason).
   *
   * After PQgetCopyData returns -1, call PQgetResult to obtain the final result status of the COPY command.
   * One can wait for this result to be available in the usual way. Then return to normal operation.
   *
   * @return either the returned row, or a return code:
   *         0 if no row available yet (only possible if async is true),
   *         -1 if end of copy (consult PQgetResult),
   *         or -2 if error (consult PQerrorMessage).
   */
  def PQgetCopyData(conn: IPGconn, async: Boolean): Either[Array[Byte], Int]

  /**
   * PQgetline - gets a newline-terminated string from the backend.
   *
   * Chiefly here so that applications can use \"COPY <rel> to stdout\" and read the
   * output string. Returns a null-terminated string in s.
   *
   * XXX this routine is now deprecated, because it can't handle binary data. If
   * called during a COPY BINARY we return EOF.
   *
   * RETURNS: EOF if error (eg, invalid arguments are given) 0 if EOL is reached
   */
  //@deprecated def PQgetline(conn: IPGconn, string: String, length: Int): Int

  /**
   * PQputline -- sends a string to the backend during COPY IN. Returns 0 if OK, EOF
   * if not.
   *
   * This is deprecated primarily because the return convention doesn't allow caller
   * to tell the difference between a hard error and a nonblock-mode send failure.
   */
  //@deprecated def PQputline(conn: IPGconn, string: String): Int

  /**
   * PQgetlineAsync - gets a COPY data row without blocking.
   *
   * This routine is for applications that want to do \"COPY <rel> to stdout\"
   * asynchronously, that is without blocking. Having issued the COPY command and
   * gotten a PGRES_COPY_OUT response, the app should call PQconsumeInput and this
   * routine until the end-of-data signal is detected. Unlike PQgetline, this routine
   * takes responsibility for detecting end-of-data.
   *
   * On each call, PQgetlineAsync will return data if a complete data row is
   * available in libpq's input buffer. Otherwise, no data is returned until the rest
   * of the row arrives.
   *
   * If -1 is returned, the end-of-data signal has been recognized (and removed from
   * libpq's input buffer). The caller *must* next call PQendcopy and then return to
   * normal processing.
   *
   * RETURNS: -1 if the end-of-copy-data marker has been recognized 0 if no data is
   * available >0 the number of bytes returned.
   *
   * The data returned will not extend beyond a data-row boundary. If possible a
   * whole row will be returned at one time. But if the buffer offered by the caller
   * is too small to hold a row sent by the backend, then a partial data row will be
   * returned. In text mode this can be detected by testing whether the last returned
   * byte is '
   * ' or not.
   *
   * The returned data is *not* null-terminated.
   */
  //@deprecated def PQgetlineAsync(conn: IPGconn, buffer: String, bufsize: Int): Int

  /**
   * PQputnbytes -- like PQputline, but buffer need not be null-terminated.
   * Returns 0 if OK, EOF if not.
   */
  //@deprecated def PQputnbytes(conn: IPGconn, buffer: String, nbytes: Int): Int

  /**
   * PQendcopy After completing the data transfer portion of a copy in/out, the
   * application must call this routine to finish the command protocol.
   *
   * When using protocol 3.0 this is deprecated; it's cleaner to use PQgetResult to
   * get the transfer status. Note however that when using 2.0 protocol, recovering
   * from a copy failure often requires a PQreset. PQendcopy will take care of that,
   * PQgetResult won't.
   *
   * RETURNS: 0 on success 1 on failure
   */
  //@deprecated def PQendcopy(conn: IPGconn): Int

  /**
   * PQsetnonblocking: sets the PGconn's database connection non-blocking if the arg
   * is true or makes it blocking if the arg is false, this will not protect you from
   * PQexec(), you'll only be safe when using the non-blocking API. Needs to be
   * called only on a connected database connection.
   *
   * @return true if successful,
   *         false if there is a problem in changing the mode of the connection.
   *
   */
  def PQsetnonblocking(conn: IPGconn, setNonBlocking: Boolean): Boolean

  /**
   * Return the blocking status of the database connection
   *
   * @return true == nonblocking, false == blocking
   */
  def PQisnonblocking(conn: IPGconn): Boolean

  def PQisthreadsafe(): Boolean
  //def PQregisterThreadLock(newhandler: CFuncPtr1[Int, Unit]): CFuncPtr1[Int, Unit]

  /**
   * PQping reports the status of the server.
   *
   * It accepts connection parameters identical to those of PQconnectdb.
   * It is not necessary to supply correct user name, password, or database name values to obtain the server status.
   * However, if incorrect values are provided, the server will log a failed connection attempt.
   *
   * @return a value of the PGPing enumeration (PQPING_OK, PQPING_REJECT, PQPING_NO_RESPONSE, PQPING_NO_ATTEMPT).
   */
  def PQping(conninfo: String): IPGPing

  /**
   * PQpingParams reports the status of the server.
   *
   * It accepts connection parameters identical to those of PQconnectdbParams.
   * It is not necessary to supply correct user name, password, or database name values to obtain the server status.
   * However, if incorrect values are provided, the server will log a failed connection attempt.
   *
   *  @return a value of the PGPing enumeration (PQPING_OK, PQPING_REJECT, PQPING_NO_RESPONSE, PQPING_NO_ATTEMPT).
   */
  def PQpingParams(params: collection.Seq[(String,String)], expandDbName: Boolean): IPGPing

  /**
   * Attempts to flush any queued output data to the server.
   *
   * Returns 0 if successful (or if the send queue is empty), -1 if it failed for some reason,
   * or 1 if it was unable to send all the data in the send queue yet
   * (this case can only occur if the connection is nonblocking).
   */
  def PQflush(conn: IPGconn): Int

  /**
   *  PQfn - Send a function call to the POSTGRES backend.
   *
   * conn : backend connection fnid : OID of function to be called result_buf :
   * pointer to result buffer result_len : actual length of result is returned here
   * result_is_int : If the result is an integer, this must be 1, otherwise this
   * should be 0 args : pointer to an array of function arguments (each has length,
   * if integer, and value/pointer) nargs : # of arguments in args array.
   *
   * RETURNS PGresult with status = PGRES_COMMAND_OK if successful. *result_len is >
   * 0 if there is a return value, 0 if not. PGresult with status = PGRES_FATAL_ERROR
   * if backend returns an error.
   *
   * NULL is returned if the column number is out of range. on communications failure. conn->errorMessage will be set.
   * ---------------------------------------------------------------
   * clear the error string
   */
  // def PQfn(conn: IPGconn, fnid: Int, result_buf: Array[Int], result_len: Array[Int], result_is_int: Int, args: Ptr[PQArgBlock], nargs: Int): IPGresult

  /**
   * Returns the result status of the command.
   *
   * @return a value of the ExecStatusType enumeration
   */
  def PQresultStatus(res: IPGresult): IExecStatusType

  /**
   * Converts the enumerated type returned by PQresultStatus into a string constant describing the status code.
   *
   * The caller should not free the result.
   */
  @deprecated("Call IExecStatusType.getName() instead",since = "0.1.0")
  def PQresStatus(status: IExecStatusType): String

  /**
   * Returns the error message associated with the command, or an empty string if there was no error.
   *
   * If there was an error, the returned string will include a trailing newline.
   * The caller should not free the result directly.
   * It will be freed when the associated PGresult handle is passed to PQclear.
   *
   * Immediately following a PQexec or PQgetResult call, PQerrorMessage (on the connection)
   * will return the same string as PQresultErrorMessage (on the result).
   * However, a PGresult will retain its error message until destroyed,
   * whereas the connection's error message will change when subsequent operations are done.
   * Use PQresultErrorMessage when you want to know the status associated with a particular PGresult.
   * Use PQerrorMessage when you want to know the status from the latest operation on the connection.
   */
  def PQresultErrorMessage(res: IPGresult): String

  /**
   * Because the caller is expected to free the result string, we must strdup any
   * constant result. We use plain strdup and document that callers should expect
   * null if out-of-memory.
   *
   * Currently, we pass this off to fe-protocol3.c in all cases; it will behave
   * reasonably sanely with an error reported by fe-protocol2.c as well. If
   * necessary, we could record the protocol version in PGresults so as to be able to
   * invoke a version-specific message formatter, but for now there's no need.
   *
   * If insufficient memory to format the message, fail cleanly
   */
  //def PQresultVerboseErrorMessage(res: IPGresult, verbosity: PGVerbosity, show_context: PGContextVisibility): String


  /**
   * Returns an individual field of an error report.
   *
   * Field values will normally not include a trailing newline. The caller should not free the result directly.
   * It will be freed when the associated PGresult handle is passed to PQclear.
   *
   * @param res a PGresult pointer
   * @param fieldIdentifier an error field identifier (member of PGErrorFieldIdentifier enum)
   * @return an error message or null if the PGresult is not an error or warning result, or does not include the specified field.
   */
  def PQresultErrorField(res: IPGresult, fieldIdentifier: IPGErrorFieldIdentifier): String

  /**
   * Returns the number of rows (tuples) in the query result.
   *
   * Note that PGresult objects are limited to no more than INT_MAX rows, so an int result is sufficient.
   */
  def PQntuples(res: IPGresult): Int

  /**
   * Returns the number of columns (fields) in each row of the query result.
   */
  def PQnfields(res: IPGresult): Int

  /**
   * Returns true if the PGresult contains binary data and false if it contains text data.
   *
   * This function is deprecated (except for its use in connection with COPY),
   * because it is possible for a single PGresult to contain text data in some columns and binary data in others.
   * PQfformat is preferred.
   *
   * @return true only if all columns of the result are binary (format 1), else false
   */
  def PQbinaryTuples(res: IPGresult): Boolean

  /**
   * Returns the column name associated with the given column number (starts at 0).
   *
   * The caller should not free the result directly.
   * It will be freed when the associated PGresult handle is passed to PQclear.
   *
   * @return null if the column number is out of range.
   */
  def PQfname(res: IPGresult, fieldNum: Int): String

  /**
   * PQfnumber: finds the column number (starts at zero) associated with the given column name.
   *
   * The column name is treated like an identifier in an SQL command, that is, it is downcased unless double-quoted.
   * But note a possible gotcha: downcasing in the frontend might follow different locale rules than downcasing in the backend...
   *
   * In the present backend it is also possible to have multiple matches, in which case the first one is found.
   *
   * @return -1 if no match, otherwise the found column number
   */
  def PQfnumber(res: IPGresult, fieldName: String): Int
  def PQftable(res: IPGresult, fieldNum: Int): IOidBox
  def PQftablecol(res: IPGresult, fieldNum: Int): Int
  def PQfformat(res: IPGresult, fieldNum: Int): Int
  def PQftype(res: IPGresult, fieldNum: Int): IOidBox
  def PQfsize(res: IPGresult, fieldNum: Int): Int
  def PQfmod(res: IPGresult, fieldNum: Int): Int

  /**
   * Returns the command status tag from the SQL command that generated the PGresult.
   *
   * Commonly this is just the name of the command, but it might include additional data such as the number of rows processed.
   * The caller should not free the result directly. It will be freed when the associated PGresult handle is passed to PQclear.
   */
  def PQcmdStatus(res: IPGresult): String

  /**
   * This function is deprecated in favor of PQoidValue and is not thread-safe.
   * It returns a string with the OID of the inserted row, while PQoidValue returns the OID value.
   */
  //@deprecated def PQoidStatus(res: IPGresult): String

  /**
   * Returns the OID of the inserted row.
   *
   * @return a valid OID if the SQL command was an INSERT that inserted exactly one row into a table that has OIDs,
   * or a EXECUTE of a prepared query containing a suitable INSERT statement.
   * Otherwise, this function returns InvalidOid (or if the table affected by the INSERT statement does not contain OIDs).
   */
  def PQoidValue(res: IPGresult): IOidBox

  /**
   * Returns the number of rows affected by the SQL command.
   *
   * This function can only be used following the execution of a SELECT, CREATE TABLE AS, INSERT, UPDATE, DELETE, MOVE, FETCH, or COPY statement,
   * or an EXECUTE of a prepared query that contains an INSERT, UPDATE, or DELETE statement.
   *
   * The caller should not free the return value directly.
   * It will be freed when the associated PGresult handle is passed to PQclear.
   *
   * @return the number of rows affected by the SQL statement that generated the PGresult,
   *         or 0 if the kind of command that generated the PGresult is invalid (see above).
   */
  def PQcmdTuples(res: IPGresult): Int

  /**
   * PQgetvalue: return the value of field 'field_num' of row 'tup_num'
   */
  def PQgetvalue(res: IPGresult, tupNum: Int, fieldNum: Int): String

  /**
   * PQgetlength: returns the actual length of a field value in bytes.
   */
  def PQgetlength(res: IPGresult, tupNum: Int, fieldNum: Int): Int

  /**
   * PQgetisnull: returns the null status of a field value.
   *
   * Row and column numbers start at 0.
   * Note that PQgetvalue will return an empty string, not a null pointer, for a null field.
   *
   * @return true if the field is null and false if it contains a non-null value.
   */
  def PQgetisnull(res: IPGresult, tupNum: Int, fieldNum: Int): Boolean

  /**
   * PQnparams: returns the number of input parameters of a prepared statement.
   */
  def PQnparams(res: IPGresult): Int

  /**
   * PQparamtype: returns type Oid of the specified statement parameter.
   */
  def PQparamtype(res: IPGresult, param_num: Int): IOidBox

  /*
  /**
   * PQdescribePrepared Obtain information about a previously prepared statement
   *
   * If the query was not even sent, return NULL; conn->errorMessage is set to a
   * relevant message. If the query was sent, a new PGresult is returned (which could
   * indicate either success or failure). On success, the PGresult contains status
   * PGRES_COMMAND_OK, and its parameter and column-heading fields describe the
   * statement's inputs and outputs respectively. The user is responsible for freeing
   * the PGresult via PQclear() when done with it.
   */
  def PQdescribePrepared(conn: IPGconn, stmt: String): IPGresult

  /**
   * PQdescribePortal Obtain information about a previously created portal
   *
   * This is much like PQdescribePrepared, except that no parameter info is returned.
   * Note that at the moment, libpq doesn't really expose portals to the client; but
   * this can be used with a portal created by a SQL DECLARE CURSOR command.
   */
  def PQdescribePortal(conn: IPGconn, portal: String): IPGresult

  /**
   * PQsendDescribePrepared Submit a Describe Statement command, but don't wait for
   * it to finish
   *
   * Returns: 1 if successfully submitted 0 if error (conn->errorMessage is set)
   */
  def PQsendDescribePrepared(conn: IPGconn, stmt: String): Int

  /**
   * PQsendDescribePortal Submit a Describe Portal command, but don't wait for it to
   * finish
   *
   * Returns: 1 if successfully submitted 0 if error (conn->errorMessage is set)
   */
  def PQsendDescribePortal(conn: IPGconn, portal: String): Int
   */

  /**
   * PQclear - free's the memory associated with a PGresult
   *
   * only send DESTROY to successfully-initialized event procs
   *
   * Free all the subsidiary blocks
   *
   * Free the top-level tuple pointer array
   *
   * zero out the pointer fields to catch programming errors
   *
   * res->curBlock was zeroed out earlier
   *
   * Free the PGresult structure itself
   */
  def PQclear(res: IPGresult): Unit

  /*
  /**
   * PQfreemem - safely frees memory allocated
   *
   * Needed mostly by Win32, unless multithreaded DLL (/MD in VC6) Used for freeing
   * memory from PQescapeBytea()/PQunescapeBytea()
   */
  def PQfreemem(ptr: Ptr[Byte]): Unit

  /**
   * PQmakeEmptyPGresult returns a newly allocated, initialized PGresult with given
   * status. If conn is not NULL and status indicates an error, the conn's
   * errorMessage is copied. Also, any PGEvents are copied from the conn.
   *
   * copy connection data we might need for operations on PGresult
   *
   * consider copying conn's errorMessage
   *
   * non-error cases
   *
   * copy events last; result must be valid if we need to PQclear
   *
   * defaults...
   */
  def PQmakeEmptyPGresult(conn: IPGconn, status: IExecStatusType): IPGresult

  /**
   * PQcopyResult
   *
   * Returns a deep copy of the provided 'src' PGresult, which cannot be NULL. The
   * 'flags' argument controls which portions of the result will or will NOT be
   * copied. The created result is always put into the PGRES_TUPLES_OK status. The
   * source result error message is not copied, although cmdStatus is.
   *
   * To set custom attributes, use PQsetResultAttrs. That function requires that
   * there are no attrs contained in the result, so to use that function you cannot
   * use the PG_COPYRES_ATTRS or PG_COPYRES_TUPLES options with this function.
   *
   * Options: PG_COPYRES_ATTRS - Copy the source result's attributes
   *
   * PG_COPYRES_TUPLES - Copy the source result's tuples. This implies copying the
   * attrs, seeing how the attrs are needed by the tuples.
   *
   * PG_COPYRES_EVENTS - Copy the source result's events.
   *
   * PG_COPYRES_NOTICEHOOKS - Copy the source result's notice hooks.
   *
   * Always copy these over. Is cmdStatus really useful here?
   *
   * Wants attrs?
   *
   * Wants to copy tuples?
   *
   * Wants to copy notice hooks?
   *
   * Wants to copy PGEvents?
   *
   * Okay, trigger PGEVT_RESULTCOPY event
   */
  def PQcopyResult(src: IPGresult, flags: Int): IPGresult

  /**
   * PQsetResultAttrs
   *
   * Set the attributes for a given result. This function fails if there are already
   * attributes contained in the provided result. The call is ignored if
   * numAttributes is zero or attDescs is NULL. If the function fails, it returns
   * zero. If the function succeeds, it returns a non-zero value.
   *
   * If attrs already exist, they cannot be overwritten.
   *
   * ignore no-op request
   *
   * deep-copy the attribute names, and determine format
   */
  def PQsetResultAttrs(res: IPGresult, numAttributes: Int, attDescs: Ptr[PGresAttDesc]): Int

  /**
   * pqResultAlloc - exported routine to allocate local storage in a PGresult.
   *
   * We force all such allocations to be maxaligned, since we don't know whether the
   * value might be binary.
   */
  def PQresultAlloc(res: IPGresult, nBytes: CSize): Ptr[Byte]

  /**
   * Sets the value for a tuple field. The tup_num must be less than or equal to
   * PQntuples(res). If it is equal, a new tuple is created and added to the result.
   * Returns a non-zero value for success and zero for failure. (On failure, we
   * report the specific problem via pqInternalNotice.)
   *
   * Note that this check also protects us against null \"res\"
   *
   * Invalid tup_num, must be <= ntups
   *
   * need to allocate a new tuple?
   *
   * initialize each column to NULL
   *
   * add it to the array
   *
   * treat either NULL_LEN or NULL value pointer as a NULL field
   *
   * Report failure via pqInternalNotice. If preceding code didn't provide an error
   * message, assume \"out of memory\" was meant.
   */
  def PQsetvalue(res: IPGresult, tup_num: Int, field_num: Int, value: String, len: Int): Int

  /**
   * force empty-string result
   */
  def PQescapeStringConn(conn: IPGconn, to: String, from: String, length: CSize, error: Ptr[Int]): CSize
  def PQescapeLiteral(conn: IPGconn, str: String, len: CSize): String
  def PQescapeIdentifier(conn: IPGconn, str: String, len: CSize): String
  def PQescapeByteaConn(conn: IPGconn, from: Ptr[CUnsignedChar], from_length: CSize, to_length: Ptr[CSize]): Ptr[CUnsignedChar]

  /**
   * PQunescapeBytea - converts the null terminated string representation of a bytea,
   * strtext, into binary, filling a buffer. It returns a pointer to the buffer (or
   * NULL on error), and the size of the buffer in retbuflen. The pointer may
   * subsequently be used as an argument to the function PQfreemem.
   *
   * The following transformations are made: \\ == ASCII 92 == \\  == a byte whose
   * value = ooo (ooo is an octal number)  == x (x is any character not matched by
   * the above transformations)
   *
   * Avoid unportable malloc(0)
   *
   * Bad input is silently ignored. Note that this includes whitespace between hex
   * pairs, which is allowed by byteain.
   *
   * Length of input is max length of output, but add one to avoid unportable
   * malloc(0) if input is zero-length.
   *
   * Note: if we see '\\' followed by something that isn't a recognized escape
   * sequence, we loop around having done nothing except advance i. Therefore the
   * something will be emitted as ordinary data on the next cycle. Corner case: '\\'
   * at end of string will just be discarded.
   *
   * buflen is the length of the dequoted data
   *
   * Shrink the buffer to be no larger than necessary
   *
   * +1 avoids unportable behavior when buflen==0
   *
   * It would only be a very brain-dead realloc that could fail, but...
   */
  def PQunescapeBytea(strtext: Ptr[CUnsignedChar], retbuflen: Ptr[CSize]): Ptr[CUnsignedChar]
  def PQescapeString(to: String, from: String, length: CSize): CSize

  /**
   * can't use hex
   */
  def PQescapeBytea(from: Ptr[CUnsignedChar], from_length: CSize, to_length: Ptr[CSize]): Ptr[CUnsignedChar]

  /**
   * PQprint()
   *
   * Format results of a query for printing.
   *
   * PQprintOpt is a typedef (structure) that contains various flags and options.
   * consult libpq-fe.h for details
   *
   * This function should probably be removed sometime since psql doesn't use it
   * anymore. It is unclear to what extent this is used by external clients, however.
   *
   * only print rows with at least 1 field.
   *
   * in case we don't use them
   *
   * If we think there'll be more than one screen of output, try to pipe to the pager
   * program.
   *
   * Since this function is no longer used by psql, we don't examine PSQL_PAGER. It's
   * possible that the hypothetical external users of the function would like that to
   * happen, but in the name of backwards compatibility, we'll stick to just
   * examining PAGER.
   *
   * if PAGER is unset, empty or all-white-space, don't use pager
   *
   * row count and newline
   *
   * ENABLE_THREAD_SAFETY
   *
   * WIN32
   *
   * ENABLE_THREAD_SAFETY
   *
   * WIN32
   */
  def PQprint(fout: Ptr[FILE], res: IPGresult, ps: Ptr[PQprintOpt]): Unit

  /**
   * really old printing routines
   *
   * Get some useful info about the results
   *
   * Figure the field lengths to align to
   *
   * will be somewhat time consuming for very large results
   *
   * first, print out the attribute names
   *
   * Underline the attribute names
   *
   * next, print out the instances
   */
  def PQdisplayTuples(res: IPGresult, fp: Ptr[FILE], fillAlign: Int, fieldSep: String, printHeader: Int, quiet: Int): Unit

  /**
   * only print rows with at least 1 field.
   */
  def PQprintTuples(res: IPGresult, fout: Ptr[FILE], printAttName: Int, terseOutput: Int, width: Int): Unit

  /**
   * lo_open opens an existing large object
   *
   * returns the file descriptor for use in later lo_* calls return -1 upon failure.
   */
  def lo_open(conn: IPGconn, lobjId: Oid, mode: Int): Int

  /**
   * lo_close closes an existing large object
   *
   * returns 0 upon success returns -1 upon failure.
   */
  def lo_close(conn: IPGconn, fd: Int): Int

  /**
   * lo_read read len bytes of the large object into buf
   *
   * returns the number of bytes read, or -1 on failure. the CALLER must have
   * allocated enough space to hold the result returned
   *
   * Long ago, somebody thought it'd be a good idea to declare this function as
   * taking size_t ... but the underlying backend function only accepts a signed
   * int32 length. So throw error if the given value overflows int32.
   */
  def lo_read(conn: IPGconn, fd: Int, buf: String, len: CSize): Int

  /**
   * lo_write write len bytes of buf into the large object fd
   *
   * returns the number of bytes written, or -1 on failure.
   *
   * Long ago, somebody thought it'd be a good idea to declare this function as
   * taking size_t ... but the underlying backend function only accepts a signed
   * int32 length. So throw error if the given value overflows int32.
   */
  def lo_write(conn: IPGconn, fd: Int, buf: String, len: CSize): Int

  /**
   * lo_lseek change the current read or write location on a large object
   */
  def lo_lseek(conn: IPGconn, fd: Int, offset: Int, whence: Int): Int

  /**
   * lo_lseek64 change the current read or write location on a large object
   */
  def lo_lseek64(conn: IPGconn, fd: Int, offset: pg_int64, whence: Int): pg_int64

  /**
   * lo_creat create a new large object the mode is ignored (once upon a time it had
   * a use)
   *
   * returns the oid of the large object created or InvalidOid upon failure
   */
  def lo_creat(conn: IPGconn, mode: Int): Oid

  /**
   * lo_create create a new large object if lobjId isn't InvalidOid, it specifies the
   * OID to (attempt to) create
   *
   * returns the oid of the large object created or InvalidOid upon failure
   *
   * Must check this on-the-fly because it's not there pre-8.1
   */
  def lo_create(conn: IPGconn, lobjId: Oid): Oid

  /**
   * lo_tell returns the current seek location of the large object
   */
  def lo_tell(conn: IPGconn, fd: Int): Int

  /**
   * lo_tell64 returns the current seek location of the large object
   */
  def lo_tell64(conn: IPGconn, fd: Int): pg_int64

  /**
   * lo_truncate truncates an existing large object to the given size
   *
   * returns 0 upon success returns -1 upon failure
   *
   * Must check this on-the-fly because it's not there pre-8.3
   *
   * Long ago, somebody thought it'd be a good idea to declare this function as
   * taking size_t ... but the underlying backend function only accepts a signed
   * int32 length. So throw error if the given value overflows int32. (A possible
   * alternative is to automatically redirect the call to lo_truncate64; but if the
   * caller wanted to rely on that backend function being available, he could have
   * called lo_truncate64 for himself.)
   */
  def lo_truncate(conn: IPGconn, fd: Int, len: CSize): Int

  /**
   * lo_truncate64 truncates an existing large object to the given size
   *
   * returns 0 upon success returns -1 upon failure
   */
  def lo_truncate64(conn: IPGconn, fd: Int, len: pg_int64): Int

  /**
   * lo_unlink delete a file
   */
  def lo_unlink(conn: IPGconn, lobjId: Oid): Int

  /**
   * lo_import - imports a file as an (inversion) large object.
   *
   * returns the oid of that object upon success, returns InvalidOid upon failure
   */
  def lo_import(conn: IPGconn, filename: String): Oid

  /**
   * lo_import_with_oid - imports a file as an (inversion) large object. large object
   * id can be specified.
   *
   * returns the oid of that object upon success, returns InvalidOid upon failure
   */
  def lo_import_with_oid(conn: IPGconn, filename: String, lobjId: Oid): Oid

  /**
   * lo_export - exports an (inversion) large object. returns -1 upon failure, 1 if
   * OK
   *
   * open the large object.
   *
   * we assume lo_open() already set a suitable error message
   *
   * create the file to be written to
   *
   * We must do lo_close before setting the errorMessage
   *
   * read in from the large object and write to the file
   *
   * We must do lo_close before setting the errorMessage
   *
   * If lo_read() failed, we are now in an aborted transaction so there's no need for
   * lo_close(); furthermore, if we tried it we'd overwrite the useful error result
   * with a useless one. So skip lo_close() if we got a failure result.
   *
   * assume lo_read() or lo_close() left a suitable error message
   *
   * if we already failed, don't overwrite that msg with a close error
   */
  def lo_export(conn: IPGconn, lobjId: Oid, filename: String): Int

  /**
   * A couple of \"miscellaneous\" multibyte related functions. They used to be in
   * fe-print.c but that file is doomed. returns the byte length of the character
   * beginning at s, using the specified encoding.
   */
  def PQmblen(s: String, encoding: Int): Int

  /**
   * returns the display length of the character beginning at s, using the specified
   * encoding.
   */
  def PQdsplen(s: String, encoding: Int): Int
  def PQenv2encoding(): Int

  /**
   * PQencryptPassword -- exported routine to encrypt a password with MD5
   *
   * This function is equivalent to calling PQencryptPasswordConn with \"md5\" as the
   * encryption method, except that this doesn't require a connection object. This
   * function is deprecated, use PQencryptPasswordConn instead.
   */
  @deprecated("use PQencryptPasswordConn instead",since="0.0.1") def PQencryptPassword(passwd: String, user: String): String

  /**
   * PQencryptPasswordConn -- exported routine to encrypt a password
   *
   * This is intended to be used by client applications that wish to send commands
   * like ALTER USER joe PASSWORD 'pwd'. The password need not be sent in cleartext
   * if it is encrypted on the client side. This is good because it ensures the
   * cleartext password won't end up in logs, pg_stat displays, etc. We export the
   * function so that clients won't be dependent on low-level details like whether
   * the encryption is MD5 or something else.
   *
   * Arguments are a connection object, the cleartext password, the SQL name of the
   * user it is for, and a string indicating the algorithm to use for encrypting the
   * password. If algorithm is NULL, this queries the server for the current
   * 'password_encryption' value. If you wish to avoid that, e.g. to avoid blocking,
   * you can execute 'show password_encryption' yourself before calling this
   * function, and pass it as the algorithm.
   *
   * Return value is a malloc'd string. The client may assume the string doesn't
   * contain any special characters that would require escaping. On error, an error
   * message is stored in the connection object, and returns NULL.
   *
   * If no algorithm was given, ask the server.
   *
   * PQexec() should've set conn->errorMessage already
   *
   * PQexec() should've set conn->errorMessage already
   *
   * Also accept \"on\" and \"off\" as aliases for \"md5\", because
   * password_encryption was a boolean before PostgreSQL 10. We refuse to send the
   * password in plaintext even if it was \"off\".
   *
   * Ok, now we know what algorithm to use
   */
  def PQencryptPasswordConn(conn: IPGconn, passwd: String, user: String, algorithm: String): String
   */

  /**
   * Search encoding by encoding name.
   *
   * Returns encoding ID, or -1 for error.
   */
  def pg_char_to_encoding(name: String): Int

  /**
   * Search encoding name by encoding ID.
   *
   * Returns encoding name, or "" if invalid encoding ID.
   */
  def pg_encoding_to_char(encoding: Int): String

  /** Client encoding check, for error returns -1 else encoding id */
  //def pg_valid_client_encoding(encoding: String): Int // ADDED BY DBO
  /** Server encoding check, for error returns -1 else encoding id */
  //def pg_valid_server_encoding(encoding: String): Int // ADDED BY DBO
  /** Server encoding ID check, for error returns -1 else encoding id */
  //def pg_valid_server_encoding_id(encoding: Int): Int // COMMENTED BY DBO

}






/*
trait ILibPQ {
  def PQconnectStart(conninfo: String): IPGconn
  def PQconnectStartParams(params: collection.Seq[(String,String)], expandDbname: Boolean): IPGconn
  def PQconnectPoll(conn: IPGconn): IPostgresPollingStatusType
  def PQconnectdb(conninfo: String): IPGconn
  def PQconnectdbParams(keywords: collection.Seq[String], values: collection.Seq[String], expand_dbname: Int): IPGconn
  def PQsetdbLogin(pghost: String, pgport: String, pgoptions: String, pgtty: String, dbName: String, login: String, pwd: String): IPGconn
  def PQfinish(conn: IPGconn): Unit
  def PQconndefaults(): IPQconninfoOption
  def PQconninfoParse(conninfo: String, errmsg: collection.Seq[String]): IPQconninfoOption
  def PQconninfo(conn: IPGconn): IPQconninfoOption
  def PQconninfoFree(connOptions: IPQconninfoOption): Unit
  def PQresetStart(conn: IPGconn): Int
  def PQresetPoll(conn: IPGconn): IPostgresPollingStatusType
  def PQreset(conn: IPGconn): Unit
  def PQgetCancel(conn: IPGconn): IPGcancel
  def PQfreeCancel(cancel: IPGcancel): Unit
  def PQcancel(cancel: IPGcancel, errbuf: String, errbufsize: Int): Int
  def PQrequestCancel(conn: IPGconn): Int
  def PQdb(conn: IPGconn): String
  def PQuser(conn: IPGconn): String
  def PQpass(conn: IPGconn): String
  def PQhost(conn: IPGconn): String
  def PQport(conn: IPGconn): String
  def PQtty(conn: IPGconn): String
  def PQoptions(conn: IPGconn): String
  def PQstatus(conn: IPGconn): IConnStatusType
  def PQtransactionStatus(conn: IPGconn): IPGTransactionStatusType
  def PQparameterStatus(conn: IPGconn, paramName: String): String
  def PQprotocolVersion(conn: IPGconn): Int
  def PQserverVersion(conn: IPGconn): Int
  def PQerrorMessage(conn: IPGconn): String
  def PQsocket(conn: IPGconn): Int
  /*
  def PQbackendPID(conn: IPGconn): Int
  def PQconnectionNeedsPassword(conn: IPGconn): Int
  def PQconnectionUsedPassword(conn: IPGconn): Int
  def PQclientEncoding(conn: IPGconn): Int
  def PQsetClientEncoding(conn: IPGconn, encoding: String): Int
  def PQsslInUse(conn: IPGconn): Int
  def PQsslStruct(conn: IPGconn, struct_name: String): Ptr[Byte]
  def PQsslAttribute(conn: IPGconn, attribute_name: String): String
  def PQsslAttributeNames(conn: IPGconn): collection.Seq[String]
  def PQgetssl(conn: IPGconn): Ptr[Byte]
  def PQinitSSL(do_init: Int): Unit
  def PQinitOpenSSL(do_ssl: Int, do_crypto: Int): Unit
  def PQsetErrorVerbosity(conn: IPGconn, verbosity: PGVerbosity): PGVerbosity
  def PQsetErrorContextVisibility(conn: IPGconn, show_context: PGContextVisibility): PGContextVisibility
  def PQtrace(conn: IPGconn, debug_port: Ptr[FILE]): Unit
  def PQuntrace(conn: IPGconn): Unit
  def PQsetNoticeReceiver(conn: IPGconn, proc: CFuncPtr2[Ptr[Byte], IPGresult, Unit], arg: Ptr[Byte]): CFuncPtr2[Ptr[Byte], IPGresult, Unit]
  def PQsetNoticeProcessor(conn: IPGconn, proc: CFuncPtr2[Ptr[Byte], String, Unit], arg: Ptr[Byte]): CFuncPtr2[Ptr[Byte], String, Unit]
  def PQregisterThreadLock(newhandler: CFuncPtr1[Int, Unit]): CFuncPtr1[Int, Unit]
  def PQexec(conn: IPGconn, query: String): IPGresult
  */
  def PQexecParams(conn: IPGconn, command: String, nParams: Int, paramTypes: IOid, paramValues: collection.Seq[String], paramLengths: Array[Int], paramFormats: Array[Int], resultFormat: Int): IPGresult
  def PQprepare(conn: IPGconn, stmtName: String, query: String, nParams: Int, paramTypes: IOid): IPGresult
  def PQexecPrepared(conn: IPGconn, stmtName: String, nParams: Int, paramValues: collection.Seq[String], paramLengths: Array[Int], paramFormats: Array[Int], resultFormat: Int): IPGresult
  def PQsendQuery(conn: IPGconn, query: String): Int
  def PQsendQueryParams(conn: IPGconn, command: String, nParams: Int, paramTypes: IOid, paramValues: collection.Seq[String], paramLengths: Array[Int], paramFormats: Array[Int], resultFormat: Int): Int
  def PQsendPrepare(conn: IPGconn, stmtName: String, query: String, nParams: Int, paramTypes: IOid): Int
  def PQsendQueryPrepared(conn: IPGconn, stmtName: String, nParams: Int, paramValues: collection.Seq[String], paramLengths: Array[Int], paramFormats: Array[Int], resultFormat: Int): Int
  def PQsetSingleRowMode(conn: IPGconn): Int
  def PQgetResult(conn: IPGconn): IPGresult
  def PQisBusy(conn: IPGconn): Int
  def PQconsumeInput(conn: IPGconn): Int
  /*
  def PQnotifies(conn: IPGconn): Ptr[PGnotify]
  def PQputCopyData(conn: IPGconn, buffer: String, nbytes: Int): Int
  def PQputCopyEnd(conn: IPGconn, errormsg: String): Int
  def PQgetCopyData(conn: IPGconn, buffer: collection.Seq[String], async: Int): Int
  def PQgetline(conn: IPGconn, string: String, length: Int): Int
  def PQputline(conn: IPGconn, string: String): Int
  def PQgetlineAsync(conn: IPGconn, buffer: String, bufsize: Int): Int
  def PQputnbytes(conn: IPGconn, buffer: String, nbytes: Int): Int
  def PQendcopy(conn: IPGconn): Int
  */
  def PQsetnonblocking(conn: IPGconn, arg: Int): Int
  def PQisnonblocking(conn: IPGconn): Int
  def PQisthreadsafe(): Int
  // def PQping(conninfo: String): PGPing
  // def PQpingParams(keywords: collection.Seq[String], values: collection.Seq[String], expand_dbname: Int): PGPing
  def PQflush(conn: IPGconn): Int
  // def PQfn(conn: IPGconn, fnid: Int, result_buf: Array[Int], result_len: Array[Int], result_is_int: Int, args: Ptr[PQArgBlock], nargs: Int): IPGresult
  def PQresultStatus(res: IPGresult): IExecStatusType
  def PQresStatus(status: IExecStatusType): String
  /*
  def PQresultErrorMessage(res: IPGresult): String
  def PQresultVerboseErrorMessage(res: IPGresult, verbosity: PGVerbosity, show_context: PGContextVisibility): String
  def PQresultErrorField(res: IPGresult, fieldcode: Int): String
  def PQntuples(res: IPGresult): Int
  def PQnfields(res: IPGresult): Int
  def PQbinaryTuples(res: IPGresult): Int
  def PQfname(res: IPGresult, field_num: Int): String
  def PQfnumber(res: IPGresult, field_name: String): Int
  def PQftable(res: IPGresult, field_num: Int): Oid
  def PQftablecol(res: IPGresult, field_num: Int): Int
  def PQfformat(res: IPGresult, field_num: Int): Int
  def PQftype(res: IPGresult, field_num: Int): Oid
  def PQfsize(res: IPGresult, field_num: Int): Int
  def PQfmod(res: IPGresult, field_num: Int): Int
  def PQcmdStatus(res: IPGresult): String
  def PQoidStatus(res: IPGresult): String
  def PQoidValue(res: IPGresult): Oid
  def PQcmdTuples(res: IPGresult): String
  def PQgetvalue(res: IPGresult, tup_num: Int, field_num: Int): String
  def PQgetlength(res: IPGresult, tup_num: Int, field_num: Int): Int
  def PQgetisnull(res: IPGresult, tup_num: Int, field_num: Int): Int
  def PQnparams(res: IPGresult): Int
  def PQparamtype(res: IPGresult, param_num: Int): Oid
  def PQdescribePrepared(conn: IPGconn, stmt: String): IPGresult
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
  def PQprintTuples(res: IPGresult, fout: Ptr[FILE], printAttName: Int, terseOutput: Int, width: Int): Unit
  def lo_open(conn: IPGconn, lobjId: Oid, mode: Int): Int
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
  def lo_export(conn: IPGconn, lobjId: Oid, filename: String): Int
  */
  def PQlibVersion(): Int
  /*
  def PQmblen(s: String, encoding: Int): Int
  def PQdsplen(s: String, encoding: Int): Int
  def PQenv2encoding(): Int
  def PQencryptPassword(passwd: String, user: String): String
  def PQencryptPasswordConn(conn: IPGconn, passwd: String, user: String, algorithm: String): String
  def pg_char_to_encoding(name: String): Int
  def pg_encoding_to_char(encoding: Int): String
  def pg_valid_server_encoding_id(encoding: Int): Int
  */
}
*/