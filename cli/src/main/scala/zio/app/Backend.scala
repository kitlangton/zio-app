package zio.app

import boopickle.Default._
import io.netty.buffer.Unpooled
import zhttp.core.ByteBuf
import zhttp.http._
import zhttp.socket.{Socket, SocketApp, WebSocketFrame}
import zio._
import zio.app.cli.protocol.{ClientCommand, ServerCommand}
import zio.clock.Clock
import zio.console._
import zio.magic._
import zio.stream.{UStream, ZStream}
import zio.duration._
import zio.random.Random

import java.nio.ByteBuffer
import scala.util.{Failure, Success, Try}

object Backend extends App {
  def randomServerMessage: ZIO[Random, Nothing, ServerCommand] =
    zio.random.nextInt.map { int =>
      ServerCommand.Message(s"MESSAGE $int")
    }

  def adminSocket: Socket[Console with Has[SbtManager] with Clock, Throwable, WebSocketFrame, WebSocketFrame] =
    pickleSocket { (command: ClientCommand) =>
      command match {
        case ClientCommand.SayThis(message) =>
          import sys.process._
          ZStream.fromEffect(UIO(s"say '$message'".!)).drain
        case ClientCommand.Subscribe =>
          println("CLIENT SUBSCRIBED")
          SbtManager.backendSbtStream
            .map(_.mkString("\n"))
            .retry(Schedule.forever)
            .map { s =>
              val command: ServerCommand = ServerCommand.State(s)
              val byteBuf                = Unpooled.wrappedBuffer(Pickle.intoBytes(command))
              WebSocketFrame.binary(ByteBuf(byteBuf))
            }
      }
    }

  private def app: Http[Any, Nothing, Any, Response[Console with Has[SbtManager] with Clock, Throwable]] =
    Http.collect { case Method.GET -> Root / "ws" =>
      Response.socket(adminSocket)
    }

  val program = for {
    port <- system.envOrElse("PORT", "8088").map(_.toInt).orElseSucceed(8088)
    _    <- putStrLn(s"STARTING SERVER ON PORT $port")
    _    <- zhttp.service.Server.start(port, app)
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    program
      .injectCustom(SbtManager.live)
      .exitCode
  }

  private def pickleSocket[R, E, A: Pickler](
      f: A => ZStream[R, E, WebSocketFrame]
  ): Socket[Console with R, E, WebSocketFrame, WebSocketFrame] =
    Socket.collect {
      case WebSocketFrame.Binary(bytes) =>
        Try(Unpickle[A].fromBytes(bytes.asJava.nioBuffer())) match {
          case Failure(error) =>
            ZStream.fromEffect(putStrErr(s"Decoding Error: $error").orDie).drain
          case Success(command) =>
            f(command)
        }
      case other =>
        ZStream.fromEffect(UIO(println(s"RECEIVED $other"))).drain
    }
}
