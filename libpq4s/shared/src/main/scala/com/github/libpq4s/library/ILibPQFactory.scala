package com.github.libpq4s.library

trait ILibPQFactory {
  def getLibPQ(): ILibPQ
}