package tui.components

import view.View.string2View
import view.{KeyEvent, View}
import tui.TerminalApp.Step
import tui.{TerminalApp, TerminalEvent}

object FancyComponent extends TerminalApp[Int, Int, Nothing] {
  override def render(state: Int): View =
    View
      .vertical(
        View.horizontal("YOUR NUMBER IS".bold, " ", state.toString.red),
        View.horizontal("YOUR NUMBER IS".underlined, " ", state.toString.cyan),
        View.horizontal("YOUR NUMBER IS".inverted, " ", state.toString.yellow)
      )
      .padding((Math.sin(state.toDouble / 300) * 20).toInt.abs, 0)
      .bordered

  override def update(state: Int, event: TerminalEvent[Int]): Step[Int, Nothing] = {
    event match {
      case TerminalEvent.UserEvent(int)             => Step.update(int)
      case TerminalEvent.SystemEvent(KeyEvent.Up)   => Step.update(state + 1)
      case TerminalEvent.SystemEvent(KeyEvent.Down) => Step.update(state - 1)
      case TerminalEvent.SystemEvent(KeyEvent.Exit) => Step.exit
      case _                                        => Step.update(state)
    }
  }
}
