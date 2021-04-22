package zio.app

import view.Input.KeyEvent
import view.{Alignment, Color, Input, View}
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.duration._
import zio.process.{Command, CommandError, ProcessInput}
import zio.stream.ZStream

import java.io.File

object ViteLaunch extends App {
  private val launchVite =
    Command("yarn", "exec", "vite")

  val program: ZIO[ZEnv, CommandError, Unit] = for {
    process <- launchVite.run
    _ <- process.stderr.lines.tap { error =>
      ZIO.when(error.exists(_.contains("Couldn't find the binary vite"))) {
        Command("yarn", "install").exitCode
      }
    }
    _ <- process.stdout.linesStream.foreach(putStrLn(_))
    _ <- console.putStrLn("DONE")
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    program.exitCode
}

object Main extends App {
  val zioSlidesDir = new File("/Users/kit/code/talks/zio-slides")

  private val launchBackend =
    Command("sbt", "--no-colors", "~ backend/reStart")

  private val launchFrontend =
    Command("sbt", "--no-colors", "~ frontend/fastLinkJS")

  private val launchVite =
    Command("yarn", "exec", "vite").stdin(ProcessInput.fromStream(ZStream.empty))

  /** TODO:
    * - Figure out how to restart failed sbt boots
    * - Abstract the run loop into a framework-ish thing
    */
  private val backendLines = ZStream
    .unwrap(UIO(launchBackend.linesStream).delay(300.millis))
    .scan(List.empty[String])((s, str) => s.prepended(str))

  private val frontendLines =
    launchFrontend.linesStream.scan(List.empty[String])((s, str) => s.prepended(str))

  val program: ZIO[Blocking with Clock with Console, Throwable, Unit] = for {
    _ <- launchVite.run
    _ <- UIO(Input.ec.alternateBuffer())
    _ <- UIO(Input.ec.clear())

    renderStream =
      backendLines
        .zipWithLatest(frontendLines)((_, _))
        .zipWithLatest(Input.terminalSizeStream)((_, _))
        .tap { case ((s1, s2), (width, height)) =>
          render(s1, s2, width, height)
        }

    interruptStream =
      ZStream.managed(
        ZManaged.make_(Input.enableRawMode)(Input.disableRawMode)
      ) *>
        ZStream.repeatEffect(Input.keypress.flatMap {
          case KeyEvent.Character('q') => ZIO.fail(new Error("OH NO"))
          case KeyEvent.Exit           => ZIO.fail(new Error("OH NO"))
          case _                       => UIO.unit
        })

    _ <- (renderStream merge interruptStream).runDrain
  } yield ()

  private def render(backendLines: List[String], frontendLines: List[String], width: Int, height: Int): UIO[Unit] = {
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

  private def renderOutput(lines: List[String], label: String, height: Int): View = {
    val frontendText = View.vertical(lines.take(height).reverse.map(View.text): _*)
    frontendText.bottomLeft.bordered.overlay(View.text(label, Color.Yellow), Alignment.bottom)
  }

  val createTemplateProject: ZIO[ZEnv, Throwable, Unit] = for {
    _    <- putStrLn(scala.Console.CYAN + "Configure your new ZIO app:" + scala.Console.RESET)
    name <- Giter8.execute
    pwd  <- system.property("user.dir").someOrFail(new Error("Can't get PWD"))
    dir = new File(new File(pwd), name)
    _ <- Command("yarn", "install").workingDirectory(dir).linesStream.foreach(putStrLn(_))
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
        View.text("zio-app", Color.Yellow)
      )
    _ <- putStrLn(view.renderNow)
  } yield ()

  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    if (args.headOption.contains("new")) {
      createTemplateProject.exitCode
    } else if (args.headOption.contains("dev")) {
      program
        .onInterrupt(
          blocking.effectBlockingIO {
            Input.exitRawModeTerminal
            Input.ec.normalBuffer()
            Input.ec.showCursor()
            println("INTERRUPTED")
          }.orDie
        )
        .ensuring(
          blocking.effectBlockingIO {
            Input.exitRawModeTerminal
            Input.ec.normalBuffer()
            Input.ec.showCursor()
            println("ENSURING")
          }.orDie
        )
        .catchAllCause { _ =>
          putStrLn(s"BYE BYE")
        }
        .exitCode
    } else {
      renderHelp.exitCode
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
}
