package $package$

import boopickle.Default._
import $package$.models.User
import io.netty.buffer.Unpooled
import zhttp.core.ByteBuf
import zhttp.http._
import zhttp.service._
import zhttp.socket._
import zio._
import zio.console._
import zio.stream.ZStream
import $package$.protocol._
import $package$.Utils.pickleSocket
import $package$.protocol.ClientCommand.Subscribe
import $package$.protocol.ServerCommand.{SendChatState, SendUserId}

import java.nio.ByteBuffer

object ChatServer extends App {
  def userSocket(user: User): Socket[Has[ChatApp] with Console, Nothing] = {
    val handleOpen =
      Socket.open { _ =>
        ZStream.fromEffect(putStrLn(s"USER CONNECTED: \${user.userName}")) *>
          ZStream.fromEffect(ChatApp.userJoined(user)).drain
      }

    val handleClose = Socket.close { _ =>
      putStrLn(s"USER DISCONNECTED: \${user.userName}") *>
        ChatApp.userLeft(user)
    }

    val handleCommand = pickleSocket { (command: ClientCommand) =>
      command match {
        case Subscribe =>
          ZStream
            .mergeAllUnbounded()(
              ChatApp.chatStateStream.map(SendChatState),
              ZStream.succeed[ServerCommand](SendUserId(user))
            )
            .map { s =>
              val bytes: ByteBuffer = Pickle.intoBytes(s)
              val byteBuf           = Unpooled.wrappedBuffer(bytes)
              WebSocketFrame.binary(ByteBuf(byteBuf))
            }
        case command =>
          ZStream.fromEffect(ChatApp.receiveCommand(user, command)).drain
      }
    }

    handleOpen ++ handleClose ++ handleCommand
  }

  private def app(config: Config): Http[Has[ChatApp] & ZEnv, Throwable] =
    Http.collect { case Method.GET -> Root / "ws" =>
      val user =
        User(
          s"\${config.usernames(scala.util.Random.nextInt(config.usernames.length))}-\${scala.util.Random.nextInt(999)}"
        )
      Response.socket(userSocket(user))
    }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = (for {
    port   <- system.envOrElse("PORT", "8088").map(_.toInt).orElseSucceed(8088)
    _      <- putStrLn(s"STARTING SERVER ON PORT \$port")
    config <- Config.service
    _      <- Server.start(port, app(config))
  } yield ())
    .provideCustomLayer(ChatApp.live ++ Config.live)
    .exitCode

}
