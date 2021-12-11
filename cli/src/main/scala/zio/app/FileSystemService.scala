package zio.app

import zio._
import zio.app.cli.protocol.FileSystemState
import zio.blocking.Blocking
import zio.magic._
import zio.nio.file.{Files, Path}
import zio.stream._
import zio.system.System

trait FileSystemService {
  def stateStream: UStream[FileSystemState]
  def cd(string: String): UIO[Unit]
}

object FileSystemService {
  def stateStream: ZStream[Has[FileSystemService], Nothing, FileSystemState] =
    ZStream.accessStream[Has[FileSystemService]](_.get.stateStream)

  def cd(string: String): ZIO[Has[FileSystemService], Nothing, Unit] =
    ZIO.serviceWith[FileSystemService](_.cd(string))

  val live: ZLayer[System with Blocking, Throwable, Has[FileSystemService]] = {
    for {
      blocking <- ZIO.service[Blocking.Service]
      system   <- ZIO.service[System.Service]
      pwd      <- system.property("user.dir").map(_.getOrElse("."))
      paths    <- Files.list(Path(pwd)).runCollect.provide(Has(blocking)).orDie
      ref      <- SubscriptionRef.make(FileSystemState(pwd, paths.map(_.toString).toList))
    } yield FileSystemServiceLive(blocking, system, ref)
  }.toLayer

  case class FileSystemServiceLive(
      blocking: zio.blocking.Blocking.Service,
      system: zio.system.System.Service,
      ref: SubscriptionRef[FileSystemState]
  ) extends FileSystemService {
    override def stateStream: UStream[FileSystemState] = ref.changes

    override def cd(directory: String): UIO[Unit] =
      for {
        paths <- Files.list(Path(directory)).runCollect.provide(Has(blocking)).orDie
        _     <- ref.ref.set(FileSystemState(directory, paths.toList.map(_.toString)))
      } yield ()
  }
}

object FSExample extends App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    (for {
      f <- FileSystemService.stateStream.foreach(state => UIO(println(state))).fork
      _ <- FileSystemService.cd("zio-app-cli-frontend")
      _ <- f.join
    } yield ())
      .injectCustom(FileSystemService.live.orDie)
      .exitCode
}
