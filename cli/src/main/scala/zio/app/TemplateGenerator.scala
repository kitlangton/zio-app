package zio.app

import zio._
import zio.blocking.{Blocking, effectBlocking}
import zio.console.Console
import zio.nio.file.Files.delete
import zio.nio.file.{Files, Path}
import zio.process.Command
import zio.stream.ZStream.fromJavaIterator
import zio.stream.{ZSink, ZStream}

import java.io.IOException
import java.nio.file.{Files => JFiles}
import java.util.Comparator

object TemplateGenerator {

  def newRecursiveDirectoryStream(dir: Path): ZStream[Blocking, Throwable, Path] = {
    val managed = ZManaged.fromAutoCloseable(
      effectBlocking(JFiles.walk(dir.toFile.toPath).sorted(Comparator.reverseOrder[java.nio.file.Path]()))
        .refineToOrDie[IOException]
    )
    ZStream
      .managed(managed)
      .mapM(dirStream => UIO(dirStream.iterator()))
      .flatMap(a => ZStream.fromJavaIterator(a))
      .map(Path.fromJava(_))
  }

  def deleteMoreRecursive(path: Path): ZIO[Blocking, Throwable, Long] =
    newRecursiveDirectoryStream(path).mapM(delete).run(ZSink.count)

  def cloneRepo: ZIO[Blocking, Exception, Path] = for {
    tempdir <- ZIO.succeed(Path("./.zio-app-g8"))
    _       <- deleteMoreRecursive(tempdir).whenM(Files.exists(tempdir)).orDie
    _       <- Files.createDirectory(tempdir).orDie
    _ <- Command("git", "clone", "https://github.com/kitlangton/zio-app")
      .workingDirectory(tempdir.toFile)
      .successfulExitCode
    templateDir = tempdir / "zio-app/cli/src/main/g8"
  } yield templateDir

  val DIM = "\u001b[2m"

  def execute: ZIO[Blocking with Console, Exception, String] = {
    for {
      cloneFiber <- cloneRepo.fork
      _ <- console.putStr(
        scala.Console.BOLD + scala.Console.GREEN + "? " + scala.Console.WHITE + "Project Name" + scala.Console.RESET + DIM + " (example) " + scala.Console.RESET
      )
      name        <- console.getStrLn.filterOrElse(_.nonEmpty)(_ => UIO("example")).map(_.trim)
      templateDir <- cloneFiber.join
      _           <- Files.move(templateDir, Path(s"./$name"))
      _           <- Renamer.rename(Path(s"./$name"), name).provideLayer(Renamer.live)
      kebabCased = name.split(" ").mkString("-").toLowerCase
    } yield kebabCased
  }
}
