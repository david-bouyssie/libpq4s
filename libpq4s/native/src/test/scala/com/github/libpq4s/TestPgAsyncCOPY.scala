package com.github.libpq4s

import com.github.libpq4s.library._
import utest._

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._

object TestPgAsyncCOPY extends TestSuite with Logging {

  // TODO: put this in a custom TestFramework
  Logging.configureLogger(minLogLevel = LogLevel.TRACE)

  private implicit val pq: ILibPQ = LibPQ

  import pq._

  private val resourceName = "postgres"
  private val _connInfo = s"host=127.0.0.1 port=5555 user=$resourceName password=$resourceName dbname=$resourceName"

  val tests = Tests {
    //'createAndPopulateCarsTable - createAndPopulateCarsTable()
    'testAsyncCOPY - testAsyncCOPY()
  }

  def now(): Duration = System.currentTimeMillis().millis

  private def testAsyncCOPY(): Future[Unit] = {

    val asyncTestStart = now()
    val nIter = 1000
    val bufferSize = 2500

    val pgConn = new AsyncConnection(_connInfo)
    val finalFuture = pgConn.open().recover { case t: Throwable =>
      logger.debug("Error during connection: " + t)
      pgConn
    } flatMap { conn =>

      conn.executeCommand("DROP TABLE IF EXISTS Cars")
      conn.executeCommand("CREATE TABLE Cars(Id INTEGER PRIMARY KEY, Name VARCHAR(20), Price INT)")

      conn.copyManager.copyIn("COPY Cars FROM STDIN").flatMap { pgBulkLoader =>

        val stringBuilder = new scala.collection.mutable.StringBuilder()
        stringBuilder.sizeHint(100000)

        //val bytesBuffer = new scala.collection.mutable.ArrayBuffer[Byte](10000000)

        //val futures = new ArrayBuffer[Future[Unit]]

        var k = 0

        // TODO: tailrec???
        @tailrec
        def writeNextRow(j: Int,maxJ: Int): Future[Unit] = {

          //println(s"j = $j")

          var i = 0
          while (i < nIter) {
            k += 1
            CopyInUtils.appendRowToStringBuilder(List(k,"Audi",52642), stringBuilder)

            //bytesBuffer ++= CopyInUtils.rowToTextBytes(List(k,"Audi",52642))

            i += 1
          }

          //println("block is ready")

          val rowAsBytes = stringBuilder.result.getBytes("UTF-8")
          stringBuilder.clear()

          /*val rowAsBytes = bytesBuffer.toArray
          bytesBuffer.clear()*/

          Await.ready(pgBulkLoader.writeToCopy(rowAsBytes), Duration.Inf)

          if (j == maxJ - 1) Future.successful( () )
          else writeNextRow(j +1, maxJ)

          /*pgBulkLoader.writeToCopy(rowAsBytes).flatMap { _ =>
            if (j == maxJ - 1) Future.successful( () )
            else writeNextRow(j +1, maxJ)
          }*/
        }

        val lastFuture = writeNextRow(1, bufferSize)

        //Future.sequence(futures).map( _ => () )

        lastFuture.map { _ =>
          pgBulkLoader.endCopy()
        }
      }
    }

    finalFuture.map { _ =>
      val took = now() - asyncTestStart
      println(s"Inserting ${nIter * bufferSize} rows using COPY took $took")

      ()
    }

  }

  /*private def _checkPQExecResult(res: IPGresult, conn: IPGconn, clearResult: Boolean = false, command: String = "Last Postgres"): Unit = {
    if (clearResult) PQclear(res)

    PGresultUtils.checkExecutionStatus(conn.underlying, res.underlying, PGRES_COMMAND_OK, clearOnMismatch = true)
  }*/

  /*private def createAndPopulateCarsTable(): Unit = {

    _tryWithConnection(autoClose = true) { conn =>

      conn.executeCommand("DROP TABLE IF EXISTS Cars")

      conn.executeCommand("CREATE TABLE Cars(Id INTEGER PRIMARY KEY, Name VARCHAR(20), Price INT)")

      for (i <- 0 to 1000) {
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
  }*/


  /*private def testAsyncCOPY(): Future[Unit] = {

    def now(): Duration = System.currentTimeMillis().millis

    val asyncTestStart = now()

    val futures = (1 to 1).map { _ =>

      val pgConn = new AsyncConnection(_connInfo)
      pgConn.open().recover { case t: Throwable =>
        logger.debug("Error during connection: " + t)
        pgConn
      } flatMap { conn =>
        assert(conn.isConnected())

        logger.debug("connected :)")

        conn.sendQuery("INSERT INTO Cars VALUES(90000,'Mazda',27770)") recover { case t: Throwable =>
          logger.debug("Error during INSERT query: " + t)
        }

        var lastFuture: Future[_] = null

        for (i <- 1 to 10) {

          conn.enableSingleRowMode()
          val f1 = conn.sendQueryAndIterate("SELECT * FROM Cars ORDER BY Name DESC LIMIT 6").flatMap { asyncRes =>
            logger.debug("received query1 results asynchronously :D")

            val nRows = asyncRes.foreachRow { row =>
              logger.debug(row.toString)
            }

            logger.debug(s"Received $nRows rows")

            conn.enableSingleRowMode()
            conn.sendQueryAndIterate("SELECT * FROM Cars ORDER BY Name, Price LIMIT 6").map { asyncRes =>
              if (asyncRes != null) {
                logger.debug("received query2 results asynchronously :D")

                val nRows = asyncRes.foreachRow { row =>
                  logger.debug(row.toString)
                }

                logger.debug(s"Received $nRows rows")
              }

              ()
            }

          } recover { case t: Throwable =>
            logger.debug("Error during first future: " + t)
            ()
          }

          conn.disableSingleRowMode()
          val f2 = conn.sendQueryAndIterate("SELECT * FROM Cars ORDER BY price ASC LIMIT 6").map { asyncRes =>
            logger.debug("received query3 results asynchronously :D")

            val nRows = asyncRes.foreachRow { row =>
              logger.debug(row.toString)
            }

            logger.debug(s"Received $nRows rows")
          } recover { case t: Throwable =>
            logger.debug("Error during SELECT query: " + t)
          }

          lastFuture = Future.sequence(List(f1,f2))
        }

        lastFuture.map { _ =>
          pgConn.close()
          ()
        }

      }
    }



    val finalFuture = Future.sequence(futures).map { _ =>

      val took = now() - asyncTestStart
      println(s"The connection and the 3 queries took $took")

      ()
    }

    //Await.ready(finalFuture, 60 seconds)

    finalFuture
  }*/


}
