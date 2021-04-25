package zio.app

import view.View.string2View
import view._
import zio._
import zio.app.TerminalApp.Step
import zio.app.internal.StringSyntax.StringOps
import zio.blocking.Blocking
import zio.console.{Console, putStrLn}
import zio.duration.durationInt
import zio.process.{Command, CommandError, ProcessInput}
import zio.stream.ZStream

import java.io.File

object Main extends App {
  private val zioSlidesDir = new File("/Users/kit/code/talks/zio-slides")

  private val launchVite = Command("yarn", "exec", "vite")
//    .workingDirectory(zioSlidesDir)
    .stdin(ProcessInput.fromStream(ZStream.empty))

  private val backendLines = runSbtCommand("~ backend/reStart")
  private val frontendLines =
    ZStream.succeed(Chunk.empty) ++ ZStream.succeed(ZIO.sleep(350.millis)).drain ++
      runSbtCommand("~ frontend/fastLinkJS")

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

  val program: ZIO[Has[TUI] with Blocking, Throwable, Unit] = for {
    _ <- launchVite.run
    events = backendLines.map(MainEvent.UpdateBackendLines) merge frontendLines.map(MainEvent.UpdateFrontendLines)
    _ <- TUI.runWithEvents(MainApp)(events, AppState(Chunk.empty, Chunk.empty))
  } yield ()

  sealed trait MainEvent

  object MainEvent {
    case class UpdateBackendLines(lines: Chunk[String])  extends MainEvent
    case class UpdateFrontendLines(lines: Chunk[String]) extends MainEvent
  }

  object MainApp extends TerminalApp[MainEvent, AppState, Nothing] {
    override def render(state: AppState): View = {

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

      View.vertical(
        stats,
        View.withSize { size =>
          val backend =
            renderOutput(state.backendLines, "BACKEND", state.maximize.contains(true), size.width, size.height)
          val frontend =
            renderOutput(state.frontendLines, "FRONTEND", state.maximize.contains(false), size.width, size.height)

          View.horizontal(frontend, backend)
        }
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

    override def update(state: AppState, event: TerminalEvent[MainEvent]): TerminalApp.Step[AppState, Nothing] =
      event match {
        case TerminalEvent.UserEvent(event) =>
          event match {
            case MainEvent.UpdateBackendLines(backendLines)   => Step.update(state.copy(backendLines = backendLines))
            case MainEvent.UpdateFrontendLines(frontendLines) => Step.update(state.copy(frontendLines = frontendLines))
          }
        case TerminalEvent.SystemEvent(KeyEvent.Exit | KeyEvent.Character('q')) => Step.exit
        case TerminalEvent.SystemEvent(KeyEvent.Character('b')) =>
          Step.update(state.focusBackend)
        case TerminalEvent.SystemEvent(KeyEvent.Character('f')) =>
          Step.update(state.focusFrontend)
        case _ => Step.update(state)

      }

  }

  case class AppState(
      backendLines: Chunk[String],
      frontendLines: Chunk[String],
      maximize: Option[Boolean] = None
  ) {

    def focusBackend: AppState = {
      val newMax = maximize match {
        case Some(true) => None
        case _          => Some(true)
      }
      copy(maximize = newMax)
    }

    def focusFrontend: AppState = {
      val newMax = maximize match {
        case Some(false) => None
        case _           => Some(false)
      }
      copy(maximize = newMax)
    }

  }

  val createTemplateProject: ZIO[ZEnv, Throwable, Unit] = for {
    _    <- putStrLn("Configure your new ZIO app.".cyan.renderNow)
    name <- Giter8.execute
    pwd  <- system.property("user.dir").someOrFail(new Error("Can't get PWD"))
    dir = new File(new File(pwd), name)
    _ <- runYarnInstall(dir)
    view = View
      .vertical(
        View
          .horizontal(
            "Created ",
            name.yellow
          )
          .padding(1, 0)
          .bordered,
        "Run the following commands to get started:",
        s"cd $name".yellow,
        "zio-app dev".yellow
      )
    _ <- putStrLn(view.renderNow)
  } yield ()

  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    if (args.headOption.contains("new")) {
      createTemplateProject.exitCode
    } else if (args.headOption.contains("dev")) {
      program
        .provideCustomLayer(TUI.live(true))
        .catchSome { case SbtError.InvalidCommand(command) =>
          val view =
            View
              .vertical(
                View
                  .vertical(
                    View.horizontal(
                      View.text("Invalid Command:", Color.Red),
                      View.text(s" ${command}")
                    )
                  )
                  .padding(1, 0)
                  .bordered
                  .overlay(
                    "ERROR".red.reversed.padding(2, 0),
                    Alignment.topLeft
                  ),
                View
                  .horizontal(
                    "Are you're sure you're running ",
                    View.text("zio-app dev", Color.Cyan),
                    " in a directory created using ",
                    View.text("zio-app new", Color.Cyan),
                    "?"
                  )
                  .padding(1, 0)
                  .bordered
              )
          putStrLn("") *>
            putStrLn(view.renderNow)
        }
        .catchAllCause { cause =>
          putStrLn("") *> putStrLn(s"BYE BYE")
        }
        .exitCode
    } else {
      renderHelp.exitCode
    }
  }

  val renderHelp: URIO[Console, Unit] = {
    import View._

    val view =
      vertical(
        horizontal(text("new", Color.Cyan), text(" Create a new zio-app")),
        horizontal(text("dev", Color.Cyan), text(" Activate live-reloading dev mode"))
      )
        .padding(1, 0)
        .bordered
        .overlay(
          View.text("commands", Color.Yellow).padding(2, 0),
          Alignment.topRight
        )

    putStrLn(view.renderNow)
  }

  private def runYarnInstall(dir: File): ZIO[Console with Blocking, CommandError, Unit] =
    Command("yarn", "install")
      .workingDirectory(dir)
      .linesStream
      .foreach(putStrLn(_))
      .tapError {
        case err if err.getMessage.contains("""Cannot run program "yarn"""") =>
          Command("npm", "i", "-g", "yarn").successfulExitCode
        case _ =>
          ZIO.unit
      }
      .retryN(1)
}
