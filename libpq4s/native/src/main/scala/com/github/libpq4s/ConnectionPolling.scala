package com.github.libpq4s

import scala.scalanative.loop._

import com.github.libpq4s.library._

private[libpq4s] trait ConnectionPolling {

  private var _connSocket: Int = 0

  /**
   * Obtain the file descriptor of the socket underlying the database connection.
   *
   * Note: Async API requires to monitor a socket (obtained from PQsocket)
   * FIXME: it doesn't work yet on Windows
   * If PQconnectStart or PQconnectStartParams succeeds, the next stage is to poll libpq so that it can proceed with the connection sequence.
   * Use PQsocket(conn) to obtain the descriptor of the socket underlying the database connection.
   * Caution: do not assume that the socket remains the same across PQconnectPoll calls.
   */
  protected def initPolling(conn: IPGconn)(implicit libpq: ILibPQ): Poll = {
    if (_connSocket > 0) scala.scalanative.posix.unistd.close(_connSocket)

    val socket = libpq.PQsocket(conn)
    assert(socket > 0) // check we obtain a valid socket ID

    /* Make a copy of the socket to avoid multiple registration of the same file descriptor */
    // If we register the same file descriptor twice on an epoll we may get get an EEXIST.
    // But it is possible to add a duplicated file descriptor to the same epoll instance (see: dup(2), dup2(2), fcntl(fd, F_DUPFD_CLOEXEC, 0)).
    // This can also be a useful technique for filtering events, if the duplicate file descriptors are registered with different events masks.
    _connSocket = scala.scalanative.posix.unistd.dup(socket)

    //println(s"initializing handle for socket ${_connSocket}")

    Poll(_connSocket)
  }

  protected def stopPolling(pollHandle: Poll): Unit = {
    //println("stopping polling")
    pollHandle.stop()
    scala.scalanative.posix.unistd.close(_connSocket)
    _connSocket = 0
  }
}
