package zio.app

import terminus.Input
import terminus.View.Color
import view.{Alignment, View}
import zio._
import zio.blocking.Blocking
import zio.console.{Console, putStrLn, putStrLnErr}
import zio.duration._
import zio.process.{Command, CommandError}
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
    Command("yarn", "exec", "vite")

  val program: ZIO[ZEnv, CommandError, Unit] = for {
    _ <- launchVite.run
    _ <- UIO(Input.ec.alternateBuffer())
    _ <- UIO(Input.hideCursor)
    _ <- UIO(Input.ec.clear())
    _ <-
      ZStream
        .unwrap(UIO(launchBackend.linesStream).delay(300.millis))
        .scan(List.empty[String])((s, str) => s.prepended(str))
        .zipWithLatest(
          launchFrontend.linesStream.scan(List.empty[String])((s, str) => s.prepended(str))
        )((_, _))
        .zipWithLatest(Input.terminalSizeStream)((_, _))
        .foreach { case ((s1, s2), (width, height)) =>
          render(s1, s2, width, height)
        }
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
      Input
        .withRawMode(program)
        .ensuring(
          UIO(Input.ec.normalBuffer()) *> UIO(Input.ec.showCursor())
        )
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
