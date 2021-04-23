package zio.app

import giter8.G8TemplateRenderer
import zio._
import zio.blocking.Blocking
import zio.console.Console
import zio.process.Command

import java.io.{File, IOException}
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

  def execute: ZIO[Blocking with Console, Exception, String] = {
    for {
      cloneFiber <- cloneRepo.fork
      _ <- console.putStr(
        scala.Console.BOLD + scala.Console.GREEN + "? " + scala.Console.WHITE + "Project Name" + scala.Console.RESET + DIM + " (example) " + scala.Console.RESET
      )
      name        <- console.getStrLn.filterOrElse(_.nonEmpty)(_ => UIO("example")).map(_.trim)
      templateDir <- cloneFiber.join
      _           <- render(templateDir, Seq(s"--name=$name", "--package=chat"))
      kebabCased = name.split(" ").mkString("-").toLowerCase
    } yield kebabCased

  }

  def render(
      templateDirectory: File,
      arguments: Seq[String]
  ): ZIO[Blocking, IOException, Either[String, String]] =
    blocking.effectBlockingIO(
      G8TemplateRenderer.render(templateDirectory, new File("."), arguments, false, None)
    )
}
