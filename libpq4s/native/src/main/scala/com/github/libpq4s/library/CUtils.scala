package com.github.libpq4s.library

import java.nio.charset.Charset

import scala.scalanative.unsafe._
import scala.scalanative.runtime.{ Array => CArray, _ }

object CUtils {

  @inline
  def fromCString(cstr: CString): String = {
    fromCString(cstr, Charset.defaultCharset())
  }

  @inline
  def fromCString(cstr: CString, charset: Charset): String = {
    if (cstr == null) return null
    scala.scalanative.unsafe.fromCString(cstr, charset)
  }

  @inline
  def toCString(str: String)(implicit z: Zone): CString = {
    toCString(str, Charset.defaultCharset())(z)
  }

  @inline
  def toCString(str: String, charset: Charset)(implicit z: Zone): CString = {
    if (str == null) null else scala.scalanative.unsafe.toCString(str, charset)
  }

  @inline def bytes2ByteArray(bytes: Ptr[Byte], len: CSize): Array[Byte] = {
    if (len == 0) return Array()

    val byteArray = ByteArray.alloc(len.toInt) // allocate memory
    val byteArrayPtr = byteArray.at(0)

    scala.scalanative.libc.string.memcpy(byteArrayPtr, bytes, len)

    byteArray.asInstanceOf[Array[Byte]]
  }

}
