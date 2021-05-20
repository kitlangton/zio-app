package $package$

import $package$.models.{ChatState, Message, User}
import $package$.protocol.ClientCommand
import zio._
import zio.clock.Clock
import zio.console.Console
import zio.stream._

/** Improvements:
  *
  * - Add a VoteStateRef. Send the current Vote State to a user
  *   when they join.
  *
  * - When a user disconnects, remove their votes.
  */
trait ChatApp {
  def chatStateStream: UStream[ChatState]

  def receiveCommand(user: User, clientCommand: ClientCommand): UIO[Unit]

  def userJoined(user: User): UIO[Unit]
  def userLeft(user: User): UIO[Unit]
}

object ChatApp {
  // Layer Definitions

  val live: URLayer[Clock with Console, Has[ChatApp]] = ChatAppLive.layer

  // Accessor Methods

  def chatStateStream: ZStream[Has[ChatApp], Nothing, ChatState] =
    ZStream.accessStream[Has[ChatApp]](_.get.chatStateStream)

  def receiveCommand(user: User, clientCommand: ClientCommand): ZIO[Has[ChatApp], Nothing, Unit] =
    ZIO.accessM[Has[ChatApp]](_.get.receiveCommand(user, clientCommand))

  def userJoined(user: User): ZIO[Has[ChatApp], Nothing, Unit] =
    ZIO.accessM[Has[ChatApp]](_.get.userJoined(user))

  def userLeft(user: User): ZIO[Has[ChatApp], Nothing, Unit] =
    ZIO.accessM[Has[ChatApp]](_.get.userLeft(user))
}

case class ChatAppLive(
    chatStateSubscriptionRef: SubscriptionRef[ChatState]
) extends ChatApp {

  def receiveCommand(user: User, clientCommand: ClientCommand): UIO[Unit] =
    clientCommand match {
      case ClientCommand.SendMessage(text) =>
        chatStateSubscriptionRef.ref
          .update(s => UIO(s.addMessage(Message(user, text))))
      case ClientCommand.Subscribe =>
        UIO.unit
    }

  override def userLeft(user: User): UIO[Unit] =
    chatStateSubscriptionRef.ref
      .update(s => UIO(s.removeUser(user)))

  override def userJoined(user: User): UIO[Unit] =
    chatStateSubscriptionRef.ref
      .update(s => UIO(s.addUser(user)))

  override def chatStateStream: UStream[ChatState] =
    chatStateSubscriptionRef.changes
}

object ChatAppLive {
  val layer: ULayer[Has[ChatApp]] = {
    for {
      chatStateSubRef <- SubscriptionRef.make(ChatState.empty)
      _               <- UIO(println("GOT IT"))
    } yield ChatAppLive(chatStateSubRef)
  }.toLayer
}
