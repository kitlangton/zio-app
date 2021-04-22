package $package$.protocol

import $package$.models.{ChatState, User}

sealed trait ServerCommand

object ServerCommand {
  case class SendChatState(chatState: ChatState) extends ServerCommand
  case class SendUserId(user: User)              extends ServerCommand
}
