package $package$

import boopickle.Default.{Pickler, Unpickle}
import zhttp.socket.{Socket, WebSocketFrame}
import zio.console.{Console, putStrErr}
import zio.stream.ZStream
import zio.{&, UIO}

import scala.util.{Failure, Success, Try}

object Utils {
  def pickleSocket[R, E, A: Pickler](f: A => ZStream[R, E, WebSocketFrame]): Socket[Console & R, E] =
    Socket.collect {
      case WebSocketFrame.Binary(bytes) =>
        Try(Unpickle[A].fromBytes(bytes.asJava.nioBuffer())) match {
          case Failure(error) =>
            ZStream.fromEffect(putStrErr(s"Decoding Error: \$error")).drain
          case Success(command) =>
            f(command)
        }
      case other =>
        ZStream.fromEffect(UIO(println(s"RECEIVED \$other"))).drain
    }
}
