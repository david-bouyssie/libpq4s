package com.github.libpq4s

abstract class LoopTestSuite extends utest.TestSuite {
  override def utestAfterEach(path: Seq[String]): Unit = {
    scala.scalanative.loop.EventLoop.run()
  }
}
