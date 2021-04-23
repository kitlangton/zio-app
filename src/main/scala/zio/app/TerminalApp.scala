package zio.app

import view.{KeyEvent, View}

trait TerminalApp[Event, State] {
  def render(state: State): View

  def update(state: State, event: TerminalEvent[Event]): State
}

sealed trait TerminalEvent[+Event]

object TerminalEvent {
  case class UserEvent[+Event](event: Event) extends TerminalEvent[Event]
  case class SystemEvent(keyEvent: KeyEvent) extends TerminalEvent[Nothing]
}
