package zio.app

import tui.TUI
import view.View._
import view._
import zio._
import zio.blocking.Blocking
import zio.process.{Command, CommandError}

import java.io.File

object Main extends App {
  def print(string: String): UIO[Unit] = UIO(println(string))

  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    if (args.headOption.contains("new"))
      createTemplateProject.exitCode
    else if (args.headOption.contains("dev"))
      DevMode.run
        .provideCustomLayer(TUI.live(true))
        .catchSome { case SbtError.InvalidCommand(command) =>
          renderInvalidCommandError(command)
        }
        .catchAllCause { cause =>
          print(cause.prettyPrint) *> print(s"BYE BYE")
        }
        .exitCode
    else
      renderHelp.exitCode

  private val createTemplateProject: ZIO[ZEnv, Throwable, Unit] = for {
    _    <- print("Configure your new ZIO app.".cyan.renderNow)
    name <- TemplateGenerator.execute
    pwd  <- system.property("user.dir").someOrFail(new Error("Can't get PWD"))
    dir = new File(new File(pwd), name)
    _ <- runYarnInstall(dir)
    view = vertical(
      horizontal("Created ", name.yellow).bordered,
      "Run the following commands to get started:",
      s"cd $name".yellow,
      "zio-app dev".yellow
    )
    _ <- print(view.renderNow)
  } yield ()

  private def renderInvalidCommandError(command: String) = {
    val view =
      vertical(
        vertical(
          horizontal(
            "Invalid Command:".red,
            s" $command"
          )
        ).bordered
          .overlay(
            "ERROR".red.reversed.padding(2, 0),
            Alignment.topLeft
          ),
        horizontal(
          "Are you're sure you're running ",
          "zio-app dev".cyan,
          " in a directory created using ",
          "zio-app new".cyan,
          "?"
        ).bordered
      )
    print("") *>
      print(view.renderNow)
  }

  private val renderHelp: UIO[Unit] = {
    val view =
      vertical(
        horizontal("new".cyan, " Create a new zio-app"),
        horizontal("dev".cyan, " Activate live-reloading dev mode")
      ).bordered
        .overlay(
          text("commands", Color.Yellow).paddingH(2),
          Alignment.topRight
        )

    print(view.renderNow)
  }

  private def runYarnInstall(dir: File): ZIO[Blocking, CommandError, Unit] =
    Command("yarn", "install")
      .workingDirectory(dir)
      .linesStream
      .foreach(print)
      .tapError {
        case err if err.getMessage.contains("""Cannot run program "yarn"""") =>
          Command("npm", "i", "-g", "yarn").successfulExitCode
        case _ =>
          ZIO.unit
      }
      .retryN(1)
}
