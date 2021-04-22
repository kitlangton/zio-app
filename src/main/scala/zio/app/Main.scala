package zio.app

import terminus.Input
import terminus.View.Color
import view.{Alignment, View}
import zio._
import zio.blocking.Blocking
import zio.console.{Console, putStrLn}
import zio.duration._
import zio.process.{Command, CommandError}
import zio.stream.ZStream
import org.apache.log4j.Category

import java.io.File

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

  val createTemplateProject: ZIO[Blocking with Console, Throwable, Unit] = for {
    _ <- putStrLn(scala.Console.CYAN + "Configure your new ZIO app:" + scala.Console.RESET)
    _ <- Giter8.execute
  } yield ()

  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    if (args.headOption.contains("new")) {
      createTemplateProject.exitCode
    } else {
      Input
        .withRawMode(program)
        .ensuring(
          UIO(Input.ec.normalBuffer()) *> UIO(Input.ec.showCursor())
        )
        .exitCode
    }

}
