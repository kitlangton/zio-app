package zio.app

import tui.TUI
import view.View._
import view._
import zio._
import zio.blocking.Blocking
import zio.console.{Console, putStrLn}
import zio.process.{Command, CommandError}

import java.io.File

object Main extends App {

  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    if (args.headOption.contains("new"))
      createTemplateProject.exitCode
    else if (args.headOption.contains("dev"))
      DevMode.run
        .provideCustomLayer(TUI.live(true))
        .catchSome { case SbtError.InvalidCommand(command) =>
          renderInvalidCommandError(command)
        }
        .catchAllCause { _ =>
          putStrLn("") *> putStrLn(s"BYE BYE")
        }
        .exitCode
    else
      renderHelp.exitCode

  private val createTemplateProject: ZIO[ZEnv, Throwable, Unit] = for {
    _    <- putStrLn("Configure your new ZIO app.".cyan.renderNow)
    name <- Giter8.execute
    pwd  <- system.property("user.dir").someOrFail(new Error("Can't get PWD"))
    dir = new File(new File(pwd), name)
    _ <- runYarnInstall(dir)
    view = vertical(
      horizontal("Created ", name.yellow).bordered,
      "Run the following commands to get started:",
      s"cd $name".yellow,
      "zio-app dev".yellow
    )
    _ <- putStrLn(view.renderNow)
  } yield ()

  private def renderInvalidCommandError(command: String) = {
    val view =
      vertical(
        vertical(
          horizontal(
            "Invalid Command:".red,
            s" $command"
          )
        )
          .padding(1, 0)
          .bordered
          .overlay(
            "ERROR".red.reversed.padding(2, 0),
            Alignment.topLeft
          ),
        horizontal(
          "Are you're sure you're running ",
          text("zio-app dev", Color.Cyan),
          " in a directory created using ",
          text("zio-app new", Color.Cyan),
          "?"
        ).bordered
      )
    putStrLn("") *>
      putStrLn(view.renderNow)
  }

  private val renderHelp: URIO[Console, Unit] = {
    val view =
      vertical(
        horizontal(text("new", Color.Cyan), text(" Create a new zio-app")),
        horizontal(text("dev", Color.Cyan), text(" Activate live-reloading dev mode"))
      ).bordered
        .overlay(
          text("commands", Color.Yellow).paddingH(2),
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
