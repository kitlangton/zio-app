package $package$.models

import scala.util.Random

case class ChatState(messages: List[Message], connectedUsers: Set[User]) {
  def addMessage(message: Message): ChatState =
    copy(messages = message :: messages)

  def addUser(user: User): ChatState = {
    val uniqueUser =
      if (connectedUsers(user))
        User(s"\${user.userName}-\${Random.nextInt(999)}")
      else user

    copy(connectedUsers = connectedUsers + uniqueUser)
  }

  def removeUser(userName: User): ChatState =
    copy(connectedUsers = connectedUsers - userName)
}

object ChatState {
  def empty: ChatState = ChatState(List.empty, Set.empty)
}
