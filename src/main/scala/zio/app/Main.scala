package zio.app

import view._
import zio._
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
    .workingDirectory(zioSlidesDir)
    .stdin(ProcessInput.fromStream(ZStream.empty))

  /** TODO:
    * - Abstract the run loop into a framework-ish thing: [[TerminalApp]]
    * - Add implicit class to ZStream for `toStream`
    */
  private val backendLines = runSbtCommand("~ backend/reStart")
  private val frontendLines =
    ZStream.succeed(Chunk.empty) ++ ZStream.succeed(ZIO.sleep(350.millis)).drain ++
      runSbtCommand("~ frontend/fastLinkJS")

  private def runSbtCommand(command: String): ZStream[ZEnv, Throwable, Chunk[String]] =
    ZStream
      .unwrap(
        for {
          process <- Command("sbt", command)
            .workingDirectory(zioSlidesDir)
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

  val program: ZIO[ZEnv, Throwable, Unit] = for {
    _ <- launchVite.run

    renderStream =
      backendLines
        .zipWithLatest(frontendLines)((_, _))
        .zipWithLatest(Input.terminalSizeStream)((_, _))
        .tap { case ((s1, s2), (width, height)) =>
          render(s1, s2, width, height)
        }

    interruptStream =
      Input.keyEventStream.collectM {
        case KeyEvent.Character('q') => ZIO.fail(new Error("OH NO"))
        case KeyEvent.Exit           => ZIO.fail(new Error("OH NO"))
      }
    _ <- (renderStream merge interruptStream).runDrain
  } yield ()

  private def render(backendLines: Chunk[String], frontendLines: Chunk[String], width: Int, height: Int): UIO[Unit] = {
    val backend  = renderOutput(backendLines, "BACKEND", height)
    val frontend = renderOutput(frontendLines, "FRONTEND", height)

    val stats =
      View
        .horizontal(
          View.text("zio-app", Color.Blue),
          View.text("  ").centerH,
          View.text("running at "),
          View.text("http://localhost:3000", Color.Cyan)
        )
        .padding(1, 0)
        .bordered
        .overlay(View.text("INFO", Color.Yellow), Alignment.bottom)

    val view     = View.vertical(stats, View.horizontal(frontend, backend))
    val rendered = view.render(width, height)

    UIO {
      Input.ec.clear()
      print("\n" + rendered)
    }
  }

  private def renderOutput(lines: Chunk[String], label: String, height: Int): View = {
    val frontendText = View.vertical(lines.takeRight(height).map(View.text): _*)
    frontendText.bottomLeft.bordered.overlay(View.text(label, Color.Yellow), Alignment.bottom)
  }

  val createTemplateProject: ZIO[ZEnv, Throwable, Unit] = for {
    _    <- putStrLn(View.text("Configure your new ZIO app.", Color.Cyan).renderNow)
    name <- Giter8.execute
    pwd  <- system.property("user.dir").someOrFail(new Error("Can't get PWD"))
    dir = new File(new File(pwd), name)
    _ <- runYarnInstall(dir)
    view = View
      .vertical(
        View
          .horizontal(
            View.text("Created "),
            View.text(name, Color.Yellow)
          )
          .padding(1, 0)
          .bordered,
        View.text("Run the following commands to get started:"),
        View.text(s"cd $name", Color.Yellow),
        View.text("zio-app dev", Color.Yellow)
      )
    _ <- putStrLn(view.renderNow)
  } yield ()

  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    if (args.headOption.contains("new")) {
      createTemplateProject.exitCode
    } else if (args.headOption.contains("dev")) {
      Input
        .withRawMode(program)
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
                  .bordered
                  .overlay(
                    View.text("ERROR", Color.Red).padding(2, 0),
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
