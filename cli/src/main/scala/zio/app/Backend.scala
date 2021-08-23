package zio.app

import boopickle.Default._
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.{HttpHeaderNames, HttpHeaderValues}
import zhttp.core.ByteBuf
import zhttp.http._
import zhttp.service.Server
import zhttp.socket.{Socket, WebSocketFrame}
import zio._
import zio.app.cli.protocol.{ClientCommand, ServerCommand}
import zio.blocking.Blocking
import zio.console._
import zio.duration._
import zio.magic._
import zio.process.{Command, CommandError}
import zio.stream.ZStream
import scala.io.Source
import scala.util.{Failure, Success, Try}

object Backend extends App {
  def appSocket
      : Socket[Console with Has[FileSystemService] with Has[SbtManager], Throwable, WebSocketFrame, WebSocketFrame] =
    pickleSocket { (command: ClientCommand) =>
      command match {
        case ClientCommand.ChangeDirectory(path) =>
          ZStream.fromEffect(FileSystemService.cd(path)).drain

        case ClientCommand.Subscribe =>
          SbtManager.launchVite merge
            SbtManager.backendSbtStream
              .zipWithLatest(SbtManager.frontendSbtStream)(_ -> _)
              .zipWithLatest(FileSystemService.stateStream)(_ -> _)
              .map { case ((b, f), fs) =>
                val command: ServerCommand = ServerCommand.State(b, f, fs)
                val byteBuf                = Unpooled.wrappedBuffer(Pickle.intoBytes(command))
                WebSocketFrame.binary(ByteBuf(byteBuf))
              }
      }
    }

  private def app: HttpApp[ZEnv with Has[FileSystemService] with Has[SbtManager], Throwable] =
    Http.collect {
      case Method.GET -> Root / "ws" =>
        Response.socket(appSocket)

      case Method.GET -> Root / "assets" / file =>
        val source = Source.fromResource(s"dist/assets/$file").getLines().mkString("\n")

        val contentTypeHtml: Header =
          if (file.endsWith(".js")) Header(HttpHeaderNames.CONTENT_TYPE, "text/javascript")
          else Header(HttpHeaderNames.CONTENT_TYPE, "text/css")

        Response.http(
          headers = List(contentTypeHtml),
          content = HttpData.CompleteData(Chunk.fromArray(source.getBytes(HTTP_CHARSET)))
        )

      case Method.GET -> Root =>
        val html = Source.fromResource(s"dist/index.html").getLines().mkString("\n")

        val contentTypeHtml: Header = Header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_HTML)
        Response.http(
          content = HttpData.CompleteData(Chunk.fromArray(html.getBytes(HTTP_CHARSET))),
          headers = List(contentTypeHtml)
        )

      case other =>
        println(s"RECEIVED NOT FOUND: $other")
        Response.status(Status.NOT_FOUND)
    }

  lazy val program = for {
    port <- system.envOrElse("PORT", "9630").map(_.toInt).orElseSucceed(9630)
    _    <- putStrLn(s"STARTING SERVER ON PORT $port")
    _    <- openBrowser.delay(1.second).fork
    _    <- Server.start(port, app)
  } yield ()

  private def openBrowser: ZIO[Blocking, CommandError, ExitCode] =
    Command("open", "http://localhost:9630").exitCode

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    program
      .injectCustom(SbtManager.live, FileSystemService.live)
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
