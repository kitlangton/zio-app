package $package$.protocol

sealed trait ClientCommand

object ClientCommand {
  case class SendMessage(text: String) extends ClientCommand
  case object Subscribe                extends ClientCommand
}
