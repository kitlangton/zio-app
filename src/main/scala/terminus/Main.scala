package terminus
import terminus.Input.KeyEvent
import terminus.View._
import zio._
import zio.stream.ZStream

import java.util.UUID

case class State(
    idx: Int,
    query: Option[String],
    options: List[String] = List("Apple", "Banana", "Spaceship", "Topiary", "Myst", "Seppuku", "Mastadon")
) {
  def +(int: Int): State = copy(idx = idx + int)
  def -(int: Int): State = copy(idx = idx - int)

  def addSearch(char: Char): State = copy(query = query.map(_ + char))
  def deleteSearch: State          = copy(query = query.map(_.dropRight(1)))
  def enableSearch: State          = copy(query = Some(""))
  def cancelSearch: State          = copy(query = None)
}

object MainComponent {
  def select(list: List[String], state: State) = {
    def highlight(string: String) = state.query match {
      case Some(value) =>
        val matching = (string zip value).takeWhile { case (c, c1) => c == c1 }.map(_._1).mkString
        HStack(Vector(text(matching).yellow.underlined, string.drop(matching.length)), 0)
      case None => text(string)
    }

    vstack(
      list.zipWithIndex.map {
        case (str, i) if i == state.idx =>
          highlight(str).red.bold
        case (str, _) =>
          highlight(str)
      }: _*
    ).bordered.bordered
  }

  def render(state: State): View =
    vstack(
      hstack("CURRENT LINE: ", (state.idx + 1).toString.red),
      select(state.options, state)
    )

  def update(key: KeyEvent)(state: State)(term: Term[State, String]) = key match {
    case KeyEvent.Character('/') if state.query.isEmpty =>
      term.update(_.enableSearch)
    case KeyEvent.Character('/') | KeyEvent.Escape =>
      term.update(_.cancelSearch)
    case KeyEvent.Character(char) if state.query.isDefined =>
      term.update(_.addSearch(char))
    case KeyEvent.Delete if state.query.isDefined =>
      term.update(_.deleteSearch)
    case KeyEvent.Character('t') =>
      term.update(_.copy(options = state.options.drop(1)))
    case KeyEvent.Character('n') =>
      term.update(_.copy(options = UUID.randomUUID().toString :: state.options))
    case KeyEvent.Enter =>
      term.succeed(state.options(state.idx))
    case KeyEvent.Character('q') | KeyEvent.Exit =>
      UIO.unit
    case KeyEvent.Character('k') | KeyEvent.Up =>
      term.update(_ - 1)
    case KeyEvent.Character('j') | KeyEvent.Down =>
      term.update(_ + 1)
    case _ =>
      UIO.unit
  }

  val component: TerminalComponent[State, String] = TerminalComponent(update, render)
}

object Main extends App {
  val program = for {
    result <- MainComponent.component.run(State(0, None))
    r2     <- MainComponent.component.run(State(4, None, List.fill(15)(result)))
    _      <- console.putStrLn(s"FINISHED WITH $result $r2")
  } yield ()

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    program
      .provideCustomLayer(TUI.live)
      .exitCode

  def run2(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    Input
      .withRawMode(
        ZStream
          .repeatEffect(Input.keypress)
          .take(10)
          .tap { k => console.putStrLn(k.toString) }
          .runDrain
      )
      .provideCustomLayer(TUI.live)
      .exitCode
}

trait Terminal[State] {
  def update: String => State
  def view: View = ???
}
