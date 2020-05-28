package com.github.libpq4s

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

import com.github.libpq4s.api._
import com.github.libpq4s.library.ILibPQ

object ConnectionFactory {

  def getLibraryVersion()(implicit libpq: ILibPQ): Int = libpq.PQlibVersion()

  private[libpq4s] def connOptionPtr2connOptions(connOptionsPtr: IPQconninfoOptionWrapper)(implicit libpq: ILibPQ): Seq[ConnInfoOption] = {
    val connOptionsBuffer = new ArrayBuffer[ConnInfoOption]()

    connOptionsPtr.forEachConnInfoOption { connInfoOpt =>
      connOptionsBuffer += connInfoOpt
    }

    if (connOptionsPtr != null)
      libpq.PQconninfoFree(connOptionsPtr)

    connOptionsBuffer
  }

  def getDefaultConnectionOptions()(implicit libpq: ILibPQ): Seq[ConnInfoOption] = {
    connOptionPtr2connOptions(libpq.PQconndefaults()) // PQconninfoFree() called internally
  }

  def parseConnectionString(connectionString: String)(implicit libpq: ILibPQ): Try[Seq[ConnInfoOption]] = {
    libpq.PQconninfoParse(connectionString).map { r =>
      connOptionPtr2connOptions(r) // PQconninfoFree() called internally
    }
  }

}
