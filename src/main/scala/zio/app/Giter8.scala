package zio.app

import giter8.G8TemplateRenderer
import zio._
import zio.blocking.Blocking
import zio.console.Console
import zio.process.Command

import java.io.File
import java.nio.file.Files

object Giter8 {

  def cloneRepo: ZIO[Blocking, Exception, File] = for {
    tempdir <- blocking.effectBlockingIO(Files.createTempDirectory("zio-app-g8").toFile)
    _ <- Command("git", "clone", "https://github.com/kitlangton/zio-app")
      .workingDirectory(tempdir)
      .successfulExitCode
    templateDir = new File(tempdir, "zio-app/src/main/g8")
  } yield templateDir

  val DIM = "\u001b[2m"

  def execute: ZIO[Blocking with Console, Throwable, Unit] = {
    for {
      cloneFiber <- cloneRepo.fork
      _ <- console.putStr(
        scala.Console.BOLD + scala.Console.GREEN + "? " + scala.Console.WHITE + "Project Name" + scala.Console.RESET + DIM + " (example) " + scala.Console.RESET
      )
      name        <- console.getStrLn.filterOrElse(_.nonEmpty)(_ => UIO("example")).map(_.trim)
      templateDir <- cloneFiber.join
      _ <- blocking.effectBlockingIO(
        render(templateDir, new File("."), Seq(s"--name=$name", "--package=chat"), false, None)
      )
    } yield ()

  }

  def render(
      templateDirectory: File,
      workingDirectory: File,
      arguments: Seq[String],
      forceOverwrite: Boolean,
      outputDirectory: Option[File]
  ): Either[String, String] =
    G8TemplateRenderer.render(templateDirectory, workingDirectory, arguments, forceOverwrite, outputDirectory)
}
