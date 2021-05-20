package zio.app

import tui.StringSyntax.StringOps
import tui.TerminalApp.Step
import tui.{TUI, TerminalApp, TerminalEvent}
import view.{Alignment, Color, KeyEvent, View}
import zio.app.DevMode.{Event, State}
import zio.blocking.Blocking
import zio.duration.durationInt
import zio.process.{Command, ProcessInput}
import zio.stream.ZStream
import zio.{Chunk, Has, ZEnv, ZIO}

import java.io.File

case class DevMode() extends TerminalApp[Event, State, Nothing] {

  override def render(state: State): View = {
    val stats =
      View
        .horizontal(
          View.text("zio-app", Color.Blue),
          View.text("  ").centerH,
          View.text("running at "),
          View.text("http://localhost:3000", Color.Cyan)
        )
        .bordered
        .overlay(View.text("INFO", Color.Yellow), Alignment.bottom)

    View
      .vertical(
        stats,
        View
          .withSize { size =>
            val backend =
              renderOutput(state.backendLines, "BACKEND", state.maximize.contains(true), size.width, size.height)
            val frontend =
              renderOutput(state.frontendLines, "FRONTEND", state.maximize.contains(false), size.width, size.height)

            View.horizontal(frontend, backend)
          }
      )
      .padding(bottom = 1)
      .top
      .overlay(
        View.horizontal("q: Quit, f: Focus Frontend, b: Focus Backend").blue,
        Alignment.bottom
      )
  }

  private def renderOutput(lines: Chunk[String], label: String, maximize: Boolean, width: Int, height: Int): View = {
    val frontendText =
      View
        .vertical(
          lines
            .takeRight(height)
            .map(_.take(width).filterNot(_.isControl))
            .map(View.text): _*
        )
        .flex(minWidth = Option.when(maximize)((width * 0.75).toInt), alignment = Alignment.bottomLeft)

    frontendText.bottomLeft.borderedTight.overlay(View.text(s" $label ", Color.Yellow), Alignment.bottom)
  }

  override def update(state: State, event: TerminalEvent[Event]): TerminalApp.Step[State, Nothing] =
    event match {
      case TerminalEvent.UserEvent(event) =>
        event match {
          case Event.UpdateBackendLines(backendLines)   => Step.update(state.copy(backendLines = backendLines))
          case Event.UpdateFrontendLines(frontendLines) => Step.update(state.copy(frontendLines = frontendLines))
        }
      case TerminalEvent.SystemEvent(KeyEvent.Exit | KeyEvent.Character('q')) => Step.exit
      case TerminalEvent.SystemEvent(KeyEvent.Character('b')) =>
        Step.update(state.focusBackend)
      case TerminalEvent.SystemEvent(KeyEvent.Character('f')) =>
        Step.update(state.focusFrontend)
      case _ => Step.update(state)
    }

}

object DevMode {
  private val zioSlidesDir = new File("/Users/kit/code/talks/zio-slides")

  private val launchVite = Command("yarn", "exec", "vite")
    //    .workingDirectory(zioSlidesDir)
    .stdin(ProcessInput.fromStream(ZStream.empty))

  private val backendLines = runSbtCommand("~ backend/reStart")
  private val frontendLines =
    ZStream.succeed(Chunk.empty) ++ ZStream.succeed(ZIO.sleep(350.millis)).drain ++
      runSbtCommand("~ frontend/fastLinkJS")

  val run: ZIO[Has[TUI] with Blocking, Throwable, Unit] = for {
    _ <- launchVite.run
    events = backendLines.map(Event.UpdateBackendLines) merge frontendLines.map(Event.UpdateFrontendLines)
    _ <- TUI.runWithEvents(DevMode())(events, State(Chunk.empty, Chunk.empty))
  } yield ()

  private def runSbtCommand(command: String): ZStream[ZEnv, Throwable, Chunk[String]] =
    ZStream
      .unwrap(
        for {
          process <- Command("sbt", command)
            //            .workingDirectory(zioSlidesDir)
            .run
            .tap(_.exitCode.fork)
          errorStream = ZStream
            .fromEffect(process.stderr.lines.flatMap { lines =>
              val errorString = lines.mkString
              if (errorString.contains("waiting for lock"))
                ZIO.fail(SbtError.WaitingForLock)
              else if (errorString.contains("Invalid commands"))
                ZIO.fail(SbtError.InvalidCommand(s"sbt $command"))
              else
                ZIO.fail(SbtError.SbtErrorMessage(errorString))
            })
        } yield ZStream.mergeAllUnbounded()(
          ZStream.succeed(s"sbt $command"),
          process.stdout.linesStream,
          errorStream
        )
      )
      .scan[Chunk[String]](Chunk.empty)(_ appended _.removingAnsiCodes)
      .catchSome { case SbtError.WaitingForLock => runSbtCommand(command) }

  sealed trait Event

  object Event {
    case class UpdateBackendLines(lines: Chunk[String])  extends Event
    case class UpdateFrontendLines(lines: Chunk[String]) extends Event
  }

  case class State(
      backendLines: Chunk[String],
      frontendLines: Chunk[String],
      maximize: Option[Boolean] = None
  ) {

    def focusBackend: State = {
      val newMax = maximize match {
        case Some(true) => None
        case _          => Some(true)
      }
      copy(maximize = newMax)
    }

    def focusFrontend: State = {
      val newMax = maximize match {
        case Some(false) => None
        case _           => Some(false)
      }
      copy(maximize = newMax)
    }

  }
}
