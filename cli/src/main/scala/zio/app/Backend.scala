//package zio.app
//
//import boopickle.Default._
//import boopickle.Pickler
//import io.netty.buffer.Unpooled
//import io.netty.handler.codec.http.{HttpHeaderNames, HttpHeaderValues}
//import zio.http._
//import zio.service.{ChannelEvent, Server}
//import zio.socket._
//import zio._
//import zio.app.cli.protocol.{ClientCommand, ServerCommand}
//import zio.process.{Command, CommandError}
//import zio.stream.ZStream
//
//import java.nio.ByteBuffer
//import scala.io.Source
//import scala.util.{Failure, Success, Try}
//
//object Backend extends ZIOAppDefault {
//  def appSocket: SocketApp[FileSystemService with SbtManager] =
//    pickleSocket { (command: ClientCommand) =>
//      command match {
//        case ClientCommand.ChangeDirectory(path) =>
//          FileSystemService.cd(path)
//
//        case ClientCommand.Subscribe =>
//          SbtManager.launchVite merge
//            SbtManager.backendSbtStream
//              .zipWithLatest(SbtManager.frontendSbtStream)(_ -> _)
//              .zipWithLatest(FileSystemService.stateStream)(_ -> _)
//              .map { case ((b, f), fs) =>
//                val command: ServerCommand = ServerCommand.State(b, f, fs)
//                val byteBuf                = Pickle.intoBytes(command)
//                WebSocketFrame.binary(Chunk.fromArray(byteBuf.array()))
//              }
//      }
//    }
//
//  implicit def chunkPickler[A](implicit aPickler: Pickler[A]): Pickler[Chunk[A]] =
//    transformPickler[Chunk[A], List[A]](as => Chunk.from(as))(_.toList)
//
//  private def app: HttpApp[FileSystemService with SbtManager, Throwable] =
//    Http.collectZIO {
//      case Method.GET -> !! / "ws" =>
//        Response.fromSocket(appSocket)
//
//      case Method.GET -> !! / "assets" / file =>
//        val source = Source.fromResource(s"dist/assets/$file").getLines().mkString("\n")
//
//        val contentTypeHtml: Header =
//          if (file.endsWith(".js")) (HttpHeaderNames.CONTENT_TYPE, "text/javascript")
//          else (HttpHeaderNames.CONTENT_TYPE, "text/css")
//
//        ZIO.succeed {
//          Response(
//            headers = Headers(contentTypeHtml),
//            data = HttpData.fromChunk(Chunk.fromArray(source.getBytes(HTTP_CHARSET))),
//          )
//        }
//
//      case Method.GET -> !! =>
//        val html = Source.fromResource(s"dist/index.html").getLines().mkString("\n")
//
//        val contentTypeHtml: Header = (HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_HTML)
//        ZIO.succeed {
//          Response(
//            data = HttpData.fromChunk(Chunk.fromArray(html.getBytes(HTTP_CHARSET))),
//            headers = Headers(contentTypeHtml),
//          )
//        }
//
//      case other =>
//        println(s"RECEIVED NOT FOUND: $other")
//        ZIO.succeed(Response.status(Status.NotFound))
//    }
//
//  lazy val program = for {
//    port <- System.envOrElse("PORT", "9630").map(_.toInt).orElseSucceed(9630)
//    _    <- Console.printLine(s"STARTING SERVER ON PORT $port")
//    _    <- openBrowser.delay(1.second).fork
//    _    <- Server.start(port, app)
//  } yield ()
//
//  private def openBrowser: ZIO[Any, CommandError, ExitCode] =
//    Command("open", "http://localhost:9630").exitCode
//
//  override def run =
//    program
//      .provide(SbtManager.live, FileSystemService.live)
//
//  private def pickleSocket[R, E, A: Pickler](
//    f: A => ZIO[R, E, Any],
//  ) =
//    Http.collectZIO[WebSocketChannelEvent] {
//
//      case ChannelEvent(ch, ChannelEvent.ChannelRead(WebSocketFrame.Binary(bytes))) =>
//        Try(Unpickle[A].fromBytes(ByteBuffer.wrap(bytes.toArray))) match {
//          case Failure(error) =>
//            Console.printLineError(s"Decoding Error: $error").orDie
//          case Success(command) =>
//            f(command)
//            }
//        }
//      case other =>
//        Console.printLineError(s"UNEXPECTED SOCKET EVENT $other")
//    }
//}
