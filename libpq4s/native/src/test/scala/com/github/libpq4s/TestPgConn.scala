package com.github.libpq4s

import scala.concurrent.duration._

import utest._

import com.github.libpq4s.api.ConnStatusType._
import com.github.libpq4s.api.ExecStatusType._
import com.github.libpq4s.library._

// TODO: create tests intercepting failures (such as invalid _connInfo)
object TestPgConn extends LoopTestSuite {

  private implicit val pq: ILibPQ = LibPQ

  private val _pgDefaultResName = "postgres"
  private val _connInfo = s"host=127.0.0.1 port=5432 user=postgres password=postgres dbname=postgres"

  val tests = Tests {
    test("testGetLibPQVersion") { testGetLibPQVersion() }
    test("testSimpleConnection") { testSimpleConnection() }
    test("testSimpleQuery") { testSimpleQuery() }

    // See:
    // - https://www.postgresql.org/docs/10/libpq-example.html
    // - https://github.com/postgres/postgres/blob/master/src/test/examples/testlibpq.c
    // - https://doxygen.postgresql.org/testlibpq_8c_source.html
    test("testOfficialExample") { testOfficialExample() }

    // See: http://zetcode.com/db/postgresqlc/
    test("testTutorial") {
      // FIXME: create the connection only once when we can combine inner tests with local definitions and lambdas
      test("CreateAndPopulateCarsTable") { _createAndPopulateCarsTable() }
      test("ListTables") { _listTables() }
      test("QueryMetadata") { _queryMetadata() }
      test("QueryMultipleRows") { _queryMultipleRows() }
      //test("QueryUsingPreparedStatement") { _queryUsingPreparedStatement() }
      //test("UpdateDataUsingTransaction") { _updateDataUsingTransaction() }
    }

    //test("testAsyncQuery") { testAsyncQuery() }
    //test("testAsyncAndIterableQuery") { testAsyncAndIterableQuery() }
  }

  def testGetLibPQVersion(): Unit = {
    println("PQ Library Version: " + ConnectionFactory.getLibraryVersion())
  }

  private def tryWithConnection(fn: Connection => Unit): Unit = {

    /* Make a connection to the database */
    val conn = new Connection(_connInfo).open()

    try {

      /* Check to see that the backend connection was successfully made */
      if (conn.checkStatus() != CONNECTION_OK) {
        throw new Exception(s"connection to database failed: ${conn.lastErrorMessage()}")
      }

      fn(conn)

    } finally {
      /* close the connection to the database and cleanup */
      conn.close()
    }
  }

  def testSimpleConnection(): Unit = {

    tryWithConnection { conn =>

      println(s"Host: ${conn.host}")
      println(s"Port: ${conn.port}")

      println(s"User: ${conn.user}")
      assert(conn.user == _pgDefaultResName)

      println(s"Database name: ${conn.databaseName}")
      assert(conn.databaseName== _pgDefaultResName)

      println(s"Password: ${conn.password}")
      assert(conn.password == _pgDefaultResName)

      val serverVersionNum = conn.getServerVersion()
      println(s"PostgreSQL Server version number: $serverVersionNum")
    }

  }

  def testSimpleQuery(): Unit = {

    tryWithConnection { conn =>

      conn.executeAndProcess("SELECT VERSION()") { res =>
        if (res.checkStatus() != PGRES_TUPLES_OK) {
          res.close()
          throw new Exception("no data retrieved: " + conn.lastErrorMessage())
        }

        val serverVersionStr = res.getFieldValue(0, 0)

        println(s"PostgreSQL Server version string: $serverVersionStr")
        //assert(serverVersionResult == serverVersionFromAPI)
      }
    }

  }

  def testOfficialExample(): Unit = {

    tryWithConnection { conn =>

      /* Set always-secure search path, so malicious users can't take control. */
      conn.executeAndProcess("SELECT pg_catalog.set_config('search_path', '', false)") { res =>
        if (res.checkStatus() != PGRES_TUPLES_OK)  {
          throw new Exception("SET failed: "+ conn.lastErrorMessage())
        }
      }

      /*
       * Our test case here involves using a cursor, for which we must be inside a transaction block.
       * We could do the whole thing with a single PQexec() of "select * from pg_database", but that's too trivial to make a good example.
       */

      /* Start a transaction block */
      conn.executeCommand("BEGIN")

      /*
       * Fetch rows from pg_database, the system catalog of databases
       */
      conn.executeCommand("DECLARE myportal CURSOR FOR select * from pg_database")

      conn.executeAndProcess("FETCH ALL in myportal") { res =>
        if (res.checkStatus() != PGRES_TUPLES_OK) {
          throw new Exception("FETCH ALL failed: "+ conn.lastErrorMessage())
        }

        /* first, print out the attribute names */
        val nCols = res.countColumns()
        for (i <- 0 until nCols) {
          print(res.findColumnName(i))
          print("\t")
        }
        println("\n")

        /* next, print out the rows */
        val nRows = res.countRows()
        for (i <- 0 until nRows) {
          for (j <- 0 until nCols) {
            print(res.getFieldValue(i, j))
            print("\t")
          }

          println("\n")
        }

      }


      /* close the portal ... we don't bother to check for errors ... */
      conn.executeCommand("CLOSE myportal")

      /* end the transaction */
      conn.executeCommand("END")
    }
  }


  /*def testTutorial(): Unit = {

    tryWithConnection { conn =>

      Tests {
        test("Creating a database table") { _createAndPopulateTable() }
      }
    }

  }*/

  /*private def _checkPQExecResult(res: QueryResult, conn: Connection, clearResult: Boolean = false, command: String = "Last Postgres"): Unit = {
    PGresultUtils.checkExecutionStatus(conn.underlying, res.underlying, PGRES_COMMAND_OK, clearOnMismatch = true)

    if (clearResult) res.close()
  }*/

  private def _createAndPopulateCarsTable(): Unit = {

    tryWithConnection { conn =>

      conn.executeCommand("DROP TABLE IF EXISTS Cars")

      conn.executeCommand("CREATE TABLE Cars(Id INTEGER PRIMARY KEY, Name VARCHAR(20), Price INT)")

      for (i <- 0 to 10) {
        val inc = i * 10

        conn.executeCommand(s"INSERT INTO Cars VALUES(${1 + inc},'Audi',52642)")

        conn.executeCommand(s"INSERT INTO Cars VALUES(${2 + inc},'Mercedes',57127)")

        conn.executeCommand(s"INSERT INTO Cars VALUES(${3 + inc},'Skoda',9000)")

        conn.executeCommand(s"INSERT INTO Cars VALUES(${4 + inc},'Volvo',29000)")

        conn.executeCommand(s"INSERT INTO Cars VALUES(${5 + inc},'Bentley',350000)")

        conn.executeCommand(s"INSERT INTO Cars VALUES(${7 + inc},'Citroen',21000)")

        conn.executeCommand(s"INSERT INTO Cars VALUES(${8 + inc},'Hummer',41400)")

        conn.executeCommand(s"INSERT INTO Cars VALUES(${9 + inc},'Volkswagen',21600)")
      }

    }
  }

  private def _checkPQSelectResult(res: QueryResult, conn: Connection): Unit = {
    if (res.checkStatus() != PGRES_TUPLES_OK) {
      res.close()
      throw new Exception("no data retrieved: " + conn.lastErrorMessage())
    }
  }

  private def _listTables(): Unit = {

    tryWithConnection { conn =>

      conn.executeAndProcess("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'") { res =>
        _checkPQSelectResult(res, conn)

        val nRows = res.countRows()
        assert(nRows == 1)

        val tableName = res.getFieldValue( 0, 0)
        assert(tableName == "cars")
      }

    }

  }


  private def _queryMetadata(): Unit = {

    tryWithConnection { conn =>

      conn.executeAndProcess("SELECT * FROM Cars WHERE Id=0") { res =>
        _checkPQSelectResult(res, conn)

        val ncols = res.countColumns()
        assert(ncols == 3)

        println(s"There are $ncols columns in the cars table")

        println("The column names are:")

        val col1 = res.findColumnName(0).getOrElse("")
        assert(col1 == "id")
        println(col1)

        val col2 = res.findColumnName(1).getOrElse("")
        assert(col2 == "name")
        println(col2)

        val col3 = res.findColumnName(2).getOrElse("")
        assert(col3 == "price")
        println(col3)
      }

    }
  }

  private def _queryMultipleRows(): Unit = {

    def now(): Duration = System.currentTimeMillis().millis

    val asyncTestStart = now()

    for (c <- 1 to 6) {

      tryWithConnection { conn =>

        for (i <- 1 to 10) {

          //val res = conn.execute("SELECT * FROM Cars LIMIT 5")
          conn.executeAndProcess("SELECT * FROM Cars ORDER BY Name DESC LIMIT 6") { res =>
            _checkPQSelectResult(res, conn)

            val nRows = res.countRows()

            for (i <- 0 until nRows) {
              val values = (0 to 2).map { j => res.getFieldValue(i, j) }
              //println(values.mkString("\t"))
            }
          }


          conn.executeAndProcess("SELECT * FROM Cars ORDER BY price ASC LIMIT 6") { res =>
            _checkPQSelectResult(res, conn)

            val nRows = res.countRows()

            for (i <- 0 until nRows) {
              val values = (0 to 2).map { j => res.getFieldValue(i, j) }
              //println(values.mkString("\t"))
            }
          }

          //println()

          conn.executeAndProcess("SELECT * FROM Cars ORDER BY Name, Price LIMIT 6") { res =>
            _checkPQSelectResult(res, conn)

            val nRows = res.countRows()

            for (i <- 0 until nRows) {
              val values = (0 to 2).map { j => res.getFieldValue(i, j) }
              //println(values.mkString("\t"))
            }
          }

          conn.executeAndIterate("SELECT * FROM Cars ORDER BY Name, Price LIMIT 6") { row =>
            println(row)
          }
        }
      }
    }

    val took = now() - asyncTestStart
    println(s"The connection and the 3 queries took $took")

  }

  private def _queryUsingPreparedStatement(): Unit = {

    tryWithConnection { conn =>

      val sql = "SELECT * FROM Cars WHERE Id=$1"
      val carId = "1"

      /*val paramValues = Array(carId)
      val paramLengths = Array(carId.length)

      val res = PQexecParams(conn, sql, 1, null, paramValues, paramLengths, null, 0)*/

      val stmt = conn.prepareTextStatement(sql)

      stmt.executeAndProcess(carId) { res =>
        _checkPQSelectResult(res, conn)

        println(
          List(
            res.getFieldValue( 0, 0),
            res.getFieldValue( 0, 1),
            res.getFieldValue( 0, 2)
          ).mkString(" ")
        )
      }

    }
  }

  private def _updateDataUsingTransaction(): Unit = {

    tryWithConnection { conn =>
      conn.executeCommand("BEGIN")
      //_checkPQExecResult(res, conn, clearResult = true, command = "BEGIN")

      val nRowsUpdated = conn.executeCommand("UPDATE Cars SET Price=23700 WHERE Id=8")
      //_checkPQExecResult(res, conn, clearResult = true, command = "UPDATE")

      assert(nRowsUpdated == 1)
      println(s"#rows affected during update: $nRowsUpdated")

      val nRowsInserted = conn.executeCommand("INSERT INTO Cars VALUES(90000,'Mazda',27770)")
      //_checkPQExecResult(res, conn, clearResult = true, command = "INSERT")

      assert(nRowsInserted == 1)
      println(s"#rows affected during insert: $nRowsInserted")

      conn.executeCommand("COMMIT")
      //_checkPQExecResult(res, conn, clearResult = true, command = "COMMIT")

      conn.executeAndProcess("SELECT * FROM Cars WHERE Id IN (8, 90000)") { res =>
        _checkPQSelectResult(res, conn)

        val nRows = res.countRows()
        assert(nRows == 2)

        val firstRow = List(
          res.getFieldValue(0, 0),
          res.getFieldValue(0, 1),
          res.getFieldValue(0, 2)
        )
        assert(firstRow == List("8","Hummer","23700"))
        println(firstRow.mkString("\t"))

        val secondRow = List(
          res.getFieldValue(1, 0),
          res.getFieldValue(1, 1),
          res.getFieldValue(1, 2)
        )
        assert(secondRow == List("90000","Mazda","27770"))
        println(secondRow.mkString("\t"))
      }

    }
  }

  /*trait IEventLoopContext {
    def poll(until: () => Boolean, thenDo: () => Unit): Unit
  }

  object WhileLoopContext extends IEventLoopContext {
    def poll(until: () => Boolean, thenDo: () => Unit): Unit = {
      var metPredicate = false
      while (!metPredicate) {
        metPredicate = until()
      }

      thenDo()
    }
  }*/

  /*
  private def _tryWithAsyncConnection(setNonBlocking: Boolean = true)(fn: IPGconn => Unit)(implicit elc: IEventLoopContext): Unit = {

    import com.github.libpq4s.api.PostgresPollingStatusType._

    /* Start an asynchronous connection */
    val conn = PQconnectStart(_connInfo)

    // TODO: https://www.postgresql.org/docs/12/libpq-connect.html
    // Requires to be able to manage to socket (obtained from PQsocket) and duplicates it with dup(fd) or fcntl(fd, F_DUPFD_CLOEXEC, 0)
    // Then we can loop like this:
    // If PQconnectPoll(conn) last returned PGRES_POLLING_READING,
    // wait until the socket is ready to read (as indicated by select(), poll(), or similar system function).
    // Then call PQconnectPoll(conn) again.
    // Conversely, if PQconnectPoll(conn) last returned PGRES_POLLING_WRITING, wait until the socket is ready to write.
    // Then call PQconnectPoll(conn) again. On the first iteration,
    // i.e. if you have yet to call PQconnectPoll, behave as if it last returned PGRES_POLLING_WRITING.
    // Continue this loop until PQconnectPoll(conn) returns PGRES_POLLING_FAILED, indicating the connection procedure has failed,
    // or PGRES_POLLING_OK, indicating the connection has been successfully made.

    try {

      /* Check to see that the backend connection was successfully made */
      if (PQstatus(conn) == CONNECTION_BAD) { // Note: CONNECTION_STARTED expected
        throw new Exception(s"connection to database failed: ${PQerrorMessage(conn)}")
      }

      /* do some work, calling PQconnectPoll from time to time */
      var status: IPostgresPollingStatusType = null

      elc.poll(
        until = { () =>
          status = PQconnectPoll(conn)
          status == PGRES_POLLING_FAILED || status == PGRES_POLLING_OK
        },
        thenDo = { () =>
          if (status == PGRES_POLLING_OK && PQsetnonblocking(conn,setNonBlocking)) {
            fn(conn)
          } else {
            throw new Exception(s"connection to database failed: ${PQerrorMessage(conn)}")
          }

          ()
        }
      )

    } finally {
      /* close the connection to the database and cleanup */
      PQfinish(conn)
    }
  }

  def _printTuples(result: IPGresult) {
    val nrows = PQntuples(result)
    val nfields = PQnfields(result)
    println(s"number of rows returned = $nrows")
    println(s"number of fields returned = $nfields")

    for(r <- 0 until nrows; n <- 0 until nfields) {
      printf(" %s = %s(%d),\n",
        PQfname(result, n),
        PQgetvalue(result, r, n),
        PQgetlength(result, r, n)
      )
    }
  }

  private def testAsyncQuery(): Unit = {

    implicit val elc: IEventLoopContext = WhileLoopContext

    _tryWithAsyncConnection() { conn =>

      // Obtain the descriptor of the socket underlying the database connection
      // doesn't work on windows
      //val socket = PQsocket(conn)
      //val socketFD = this.createFD(socket)

      // A typical async application will have a main loop that uses select() or poll() to react to read and write events.
      // The socket will signal activity when there is back-end data to be processed,
      // but there are a couple of conditions that must be met for this to work.
      //
      // 1. There must be no data outstanding to be sent to the server. We can ensure this by calling PQflush until it returns zero.
      // After sending any command or data on a nonblocking connection, call PQflush.
      // If it returns 1, wait for the socket to become read- or write-ready.
      // If it becomes write-ready, call PQflush again. If it becomes read-ready, call PQconsumeInput, then call PQflush again.
      // Repeat until PQflush returns 0.
      // It is necessary to check for read-ready and drain the input with PQconsumeInput,
      // because the server can block trying to send us data (e.g. NOTICE messages) and won't read our data until we read its.
      //
      // 2. There must be input available from the server, which in terms of select() means readable data on the file descriptor identified by PQsocket.
      // Once PQflush returns 0, wait for the socket to be read-ready and then read the response.
      //
      // 3. Data must be read from the connection by PQconsumeInput() before calling PQgetResult().
      // When the main loop detects input ready, it should call PQconsumeInput to read the input.
      // It can then call PQisBusy, followed by PQgetResult if PQisBusy returns false (0).

      // Check that current connection is set to nonblocking mode
      assert(PQisnonblocking(conn))

      // Submit query to the server without waiting for the results
      PQsendQuery(conn, "SELECT * FROM Cars")

      var result: IPGresult = null
      elc.poll(
        until = { () =>
          result = PQgetResult(conn)
          result != null
        },
        thenDo = { () =>
          _checkPQSelectResult(result, conn)
          _printTuples(result)
          PQclear(result)
        }
      )
    }
  }

  private def testAsyncAndIterableQuery(): Unit = {

    implicit val elc: IEventLoopContext = WhileLoopContext

    _tryWithAsyncConnection() { conn =>

      // Check that current connection is set to nonblocking mode
      assert(PQisnonblocking(conn))

      // Submit query to the server without waiting for the results
      PQsendQuery(conn, "SELECT * FROM Cars")

      // Select single-row mode for the currently-executing query
      PQsetSingleRowMode(conn)

      var allResultsProcessed = false
      while (!allResultsProcessed) {

        var result: IPGresult = null
        elc.poll(
          until = { () =>
            result = PQgetResult(conn)
            result != null
          },
          thenDo = { () =>

            val resultStatus = PQresultStatus(result)
            if (resultStatus == PGRES_SINGLE_TUPLE) {
              _printTuples(result)
              println()
              PQclear(result)
            } else if (resultStatus == PGRES_TUPLES_OK) {
              PQclear(result)
              allResultsProcessed = true
            } else {
              PQclear(result)
              throw new Exception(s"bad PQresultStatus ($resultStatus): " + PQerrorMessage(conn))
            }

          }
        )
      }

    }
  }*/

  /*
  def htonl(x: Int): Array[Byte] = {

    var localX = x
    val res = new Array[Byte](4)

    var i = 0
    while ( {i < 4}) {
      res(i) = new Integer(x >>> 24).byteValue
      localX <<= 8

      i += 1
    }

    res
  }

  def ntohl(x: Array[Byte]): Int = {
    var res = 0

    var i = 0
    while ( {i < 4}) {
      res <<= 8
      res |= x(i).toInt

      i += 1
    }

    res
  }*/

  /*
  // Doesn't work on windows
  def createFD(socket: Int): java.io.FileDescriptor = {

    import java.io.FileDescriptor
    import java.lang.reflect.Constructor
    val clazz = classOf[FileDescriptor]

    /*val c: Constructor[FileDescriptor] = clazz.getDeclaredConstructor(classOf[java.lang.Integer]) // java.lang.Integer.TYPE
    c.setAccessible(true)
    c.newInstance(new Integer(socket))*/

    val ctor = classOf[FileDescriptor].getDeclaredConstructor(Integer.TYPE)
    ctor.setAccessible(true)

    val fd = ctor.newInstance(new java.lang.Integer(socket))
    ctor.setAccessible(false)

    fd
  }*/

}
