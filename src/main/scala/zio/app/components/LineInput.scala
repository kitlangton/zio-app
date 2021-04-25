package zio.app.components

import view.View.string2View
import view.{KeyEvent, View}
import zio.app.{TerminalApp, TerminalEvent}
import zio.app.TerminalApp.Step

object LineInput extends TerminalApp[Any, String, String] {
  override def render(state: String): View =
    View.horizontal("-> ".bold.green, state.bold, View.text(" ").reversed)

  override def update(state: String, event: TerminalEvent[Any]): Step[String, String] = {
    event match {
      case TerminalEvent.SystemEvent(KeyEvent.Character(c)) => Step.update(state + c)
      case TerminalEvent.SystemEvent(KeyEvent.Delete)       => Step.update(state.dropRight(1))
      case TerminalEvent.SystemEvent(KeyEvent.Exit)         => Step.exit
      case TerminalEvent.SystemEvent(KeyEvent.Enter)        => Step.succeed(state)
      case _                                                => Step.update(state)
    }
  }
}
