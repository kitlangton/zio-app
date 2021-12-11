package zio.app

import zio.blocking.Blocking
import zio.nio.file.{Files, Path}
import zio.{Has, IO, UIO, URLayer, ZIO}

import java.io.IOException

trait Renamer {
  def renameFolders(path: Path): IO[IOException, Unit]

  def renameFiles(path: Path, name: String): IO[IOException, Unit]

  def renameFile(path: Path, name: String): IO[IOException, Unit]

  def printTree(path: Path): IO[IOException, Unit]
}

object Renamer {
  val live: URLayer[Blocking, Has[Renamer]] = RenamerLive.toLayer[Renamer]

  def rename(path: Path, name: String): ZIO[Has[Renamer], IOException, Unit] =
    renameFolders(path) *> renameFiles(path, name)

  def renameFolders(path: Path): ZIO[Has[Renamer], IOException, Unit] =
    ZIO.serviceWith[Renamer](_.renameFolders(path))

  def renameFiles(path: Path, name: String): ZIO[Has[Renamer], IOException, Unit] =
    ZIO.serviceWith[Renamer](_.renameFiles(path, name))

  def printTree(path: Path): ZIO[Has[Renamer], IOException, Unit] =
    ZIO.serviceWith[Renamer](_.printTree(path))
}

case class RenamerLive(blocking: zio.blocking.Blocking.Service) extends Renamer {
  override def renameFolders(path: Path): IO[IOException, Unit] =
    Files
      .walk(path)
      .filterM { path => Files.isDirectory(path).map { _ && path.endsWith(Path("$package$")) } }
      .runCollect
      .flatMap { paths =>
        ZIO.foreach_(paths) { path =>
          val newPath = path.toString().replace("$package$", "example")
          Files.move(path, Path(newPath))
        }
      }
      .provide(Has(blocking))

  override def renameFiles(path: Path, name: String): IO[IOException, Unit] =
    Files
      .walk(path)
      .filterM(Files.isRegularFile(_))
      .foreach(renameFile(_, name))
      .provide(Has(blocking))

  override def renameFile(path: Path, name: String): IO[IOException, Unit] = (for {
    lines <- Files.readAllLines(path).map(_.mkString("\n"))
    newLines = lines
      .replace("$package$", "example")
      .replace("$name$", name)
      .replace("$description$", "A full-stack Scala application powered by ZIO and Laminar.")
    _ <- Files.writeLines(path, newLines.split("\n"))
  } yield ())
    .provide(Has(blocking))

  override def printTree(path: Path): IO[IOException, Unit] =
    Files
      .walk(path)
      .foreach { path =>
        ZIO.whenM(Files.isDirectory(path)) {
          UIO(println(path))
        }
      }
      .provide(Has(blocking))

}
