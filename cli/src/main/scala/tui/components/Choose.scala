package tui.components

import zio._
import view.View.string2View
import view.{KeyEvent, View}
import tui.TerminalApp.Step
import tui.{TUI, TerminalApp, TerminalEvent}

case class Choose[A](renderA: A => View) extends TerminalApp[Any, Choose.State[A], A] {
  override def render(state: Choose.State[A]): View = {
    val renderedViews = state.options.zipWithIndex.map { case (option, idx) =>
      val cursor =
        if (state.index == idx) "> ".green.bold
        else View.text("  ")
      View.horizontal(cursor, renderA(option))
    }

    View
      .vertical(
        ("CHOOSE".green :: renderedViews): _*
      )

  }

  override def update(state: Choose.State[A], event: TerminalEvent[Any]): Step[Choose.State[A], A] = {
    event match {
      case TerminalEvent.SystemEvent(KeyEvent.Up)    => Step.update(state.moveUp)
      case TerminalEvent.SystemEvent(KeyEvent.Down)  => Step.update(state.moveDown)
      case TerminalEvent.SystemEvent(KeyEvent.Enter) => Step.succeed(state.current)
      case TerminalEvent.SystemEvent(KeyEvent.Exit)  => Step.exit
      case _                                         => Step.update(state)
    }
  }
}

object Choose {
  def run[A](options: List[A])(render: A => View): RIO[TUI, Option[A]] = {
    val value = Choose[A](render)
    TUI.run(value)(State(options))
  }

  case class State[A](options: List[A], index: Int = 0) {
    def current: A         = options(index)
    def moveUp: State[A]   = changeIndex(-1)
    def moveDown: State[A] = changeIndex(1)

    def changeIndex(delta: Int): State[A] = copy(index = (index + delta) max 0 min (options.length - 1))
  }
}
