package zio.app

import zio.nio.file.{Files, Path}
import zio._

import java.io.IOException

trait Renamer {
  def renameFolders(path: Path): IO[IOException, Unit]

  def renameFiles(path: Path, name: String): IO[IOException, Unit]

  def renameFile(path: Path, name: String): IO[IOException, Unit]

  def printTree(path: Path): IO[IOException, Unit]
}

object Renamer {
  val live: URLayer[Any, Renamer] = ZLayer.succeed(RenamerLive())

  def rename(path: Path, name: String): ZIO[Renamer, IOException, Unit] =
    renameFolders(path) *> renameFiles(path, name)

  def renameFolders(path: Path): ZIO[Renamer, IOException, Unit] =
    ZIO.serviceWith[Renamer](_.renameFolders(path))

  def renameFiles(path: Path, name: String): ZIO[Renamer, IOException, Unit] =
    ZIO.serviceWith[Renamer](_.renameFiles(path, name))

  def printTree(path: Path): ZIO[Renamer, IOException, Unit] =
    ZIO.serviceWith[Renamer](_.printTree(path))
}

case class RenamerLive() extends Renamer {
  override def renameFolders(path: Path): IO[IOException, Unit] =
    Files
      .walk(path)
      .filterZIO { path => Files.isDirectory(path).map { _ && path.endsWith(Path("$package$")) } }
      .runCollect
      .flatMap { paths =>
        ZIO.foreachDiscard(paths) { path =>
          val newPath = path.toString().replace("$package$", "example")
          Files.move(path, Path(newPath))
        }
      }

  override def renameFiles(path: Path, name: String): IO[IOException, Unit] =
    Files
      .walk(path)
      .filterZIO(Files.isRegularFile(_))
      .foreach(renameFile(_, name))

  override def renameFile(path: Path, name: String): IO[IOException, Unit] = (for {
    lines <- Files.readAllLines(path).map(_.mkString("\n"))
    newLines = lines
      .replace("$package$", "example")
      .replace("$name$", name)
      .replace("$description$", "A full-stack Scala application powered by ZIO and Laminar.")
    _ <- Files.writeLines(path, newLines.split("\n"))
  } yield ())

  override def printTree(path: Path): IO[IOException, Unit] =
    Files
      .walk(path)
      .foreach { path =>
        ZIO.whenZIO(Files.isDirectory(path)) {
          ZIO.succeed(println(path))
        }
      }

}
