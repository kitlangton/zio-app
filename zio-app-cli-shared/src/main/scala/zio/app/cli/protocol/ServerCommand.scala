package zio.app.cli.protocol

sealed trait ServerCommand

object ServerCommand {
  case class Message(string: String)     extends ServerCommand
  case class State(backendLines: String) extends ServerCommand
}

sealed trait ClientCommand

object ClientCommand {
  case object Subscribe           extends ClientCommand
  case class SayThis(str: String) extends ClientCommand
}
