package zio.app

import zio._
import zio.app.cli.protocol.FileSystemState
import zio.nio.file.{Files, Path}
import zio.stream._

trait FileSystemService {
  def stateStream: UStream[FileSystemState]
  def cd(string: String): UIO[Unit]
}

object FileSystemService {
  def stateStream: ZStream[FileSystemService, Nothing, FileSystemState] =
    ZStream.environmentWithStream[FileSystemService](_.get.stateStream)

  def cd(string: String): ZIO[FileSystemService, Nothing, Unit] =
    ZIO.serviceWith[FileSystemService](_.cd(string))

  val live: ZLayer[Any, Throwable, FileSystemService] = ZLayer {
    for {
      pwd   <- System.property("user.dir").map(_.getOrElse("."))
      paths <- Files.list(Path(pwd)).runCollect.orDie
      ref   <- SubscriptionRef.make(FileSystemState(pwd, paths.map(_.toString).toList))
    } yield FileSystemServiceLive(ref)
  }

  case class FileSystemServiceLive(
      ref: SubscriptionRef[FileSystemState]
  ) extends FileSystemService {
    override def stateStream: UStream[FileSystemState] = ref.changes

    override def cd(directory: String): UIO[Unit] =
      for {
        paths <- Files.list(Path(directory)).runCollect.orDie
        _     <- ref.set(FileSystemState(directory, paths.toList.map(_.toString)))
      } yield ()
  }
}

object FSExample extends ZIOAppDefault {
  override def run =
    (for {
      f <- FileSystemService.stateStream.foreach(state => ZIO.succeed(println(state))).fork
      _ <- FileSystemService.cd("zio-app-cli-frontend")
      _ <- f.join
    } yield ())
      .provide(FileSystemService.live.orDie)
}
