package zio.app.cli.protocol

import zio.Chunk

case class FileSystemState(pwd: String, dirs: List[String])

sealed trait ServerCommand

object ServerCommand {
  case class State(
      backendLines: Chunk[Line],
      frontendLines: Chunk[Line],
      fileSystemState: FileSystemState
  ) extends ServerCommand
}

sealed trait ClientCommand

object ClientCommand {
  case object Subscribe                    extends ClientCommand
  case class ChangeDirectory(path: String) extends ClientCommand
}

case class Fragment(string: String, attributes: Chunk[Attribute])
case class Line(fragments: Chunk[Fragment])

sealed trait Attribute

object Attribute {
  case object Red     extends Attribute
  case object Yellow  extends Attribute
  case object Blue    extends Attribute
  case object Green   extends Attribute
  case object Magenta extends Attribute
  case object Cyan    extends Attribute
  case object Bold    extends Attribute
}
