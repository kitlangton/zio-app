package zio.app

import terminus.Input
import terminus.View.Color
import view.{Alignment, View}
import zio._
import zio.blocking.Blocking
import zio.console.Console
import zio.process.{Command, CommandError}
import zio.stream.ZStream

import java.io.File

object WebRunner extends App {
  val zioSlidesDir = new File("/Users/kit/code/talks/zio-slides")

  private val launchBackend =
    Command("sbt", "--no-colors", "~ backend/reStart").workingDirectory(zioSlidesDir)

  private val launchFrontend =
    Command("sbt", "--no-colors", "~ frontend/fastLinkJS").workingDirectory(zioSlidesDir)

  private val launchVite =
    Command("vite").workingDirectory(zioSlidesDir)

  val program: ZIO[Blocking with Console, CommandError, Unit] = for {
    _ <- launchVite.run
    _ <- UIO(Input.ec.alternateBuffer())
    _ <- UIO(Input.ec.clear())
    _ <-
      launchBackend.linesStream
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

  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    Input
      .withRawMode(program)
      .ensuring(
        Input.disableRawMode *> UIO(Input.ec.normalBuffer()) *> UIO(Input.ec.showCursor())
      )
      .exitCode

}
