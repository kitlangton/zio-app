package zio.app

sealed trait SbtError extends Throwable

object SbtError {
  case object WaitingForLock extends SbtError

  case class InvalidCommand(command: String) extends SbtError {
    override def getMessage: String =
      s"Invalid Sbt Command: $command"
  }

  case class SbtErrorMessage(message: String) extends SbtError {
    override def getMessage: String =
      s"SbtErrorMessage: $message"
  }
}
