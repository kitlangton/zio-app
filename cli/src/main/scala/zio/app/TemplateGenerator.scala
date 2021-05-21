package zio.app

import zio._
import zio.blocking.Blocking
import zio.console.Console
import zio.nio.core.file.Path
import zio.nio.file.Files
import zio.process.Command

object TemplateGenerator {
  def cloneRepo: ZIO[Blocking, Exception, Path] = for {
    tempdir <- Files.createTempDirectory(Some("zio-app-g8"), List.empty).orDie
    _ <- Command("git", "clone", "https://github.com/kitlangton/zio-app")
      .workingDirectory(tempdir.toFile)
      .successfulExitCode
    templateDir = (tempdir / "zio-app/cli/src/main/g8")
    _ <- UIO(println(templateDir))
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
