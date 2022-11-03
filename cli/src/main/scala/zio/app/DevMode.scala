package zio.app

import zio.process.{Command, ProcessInput}
import zio.stream.{Stream, ZStream}
import zio._

object DevMode {
  val launchVite = Command("yarn", "exec", "vite")
    .stdin(ProcessInput.fromStream(ZStream.empty))

  val backendLines: Stream[Throwable, String] =
    runSbtCommand("~ backend/reStart")

  val frontendLines: Stream[Throwable, String] =
    ZStream.succeed("") ++
      ZStream.succeed(ZIO.sleep(350.millis)).drain ++
      runSbtCommand("~ frontend/fastLinkJS")

  def runSbtCommand(command: String): Stream[SbtError, String] =
    ZStream
      .unwrap(
        for {
          process <- Command("sbt", command, "--color=always").run
                       .tap(_.exitCode.fork)
          errorStream = ZStream
                          .fromZIO(process.stderr.lines.flatMap { lines =>
                            val errorString = lines.mkString
                            if (errorString.contains("waiting for lock"))
                              ZIO.fail(SbtError.WaitingForLock)
                            else if (errorString.contains("Invalid commands"))
                              ZIO.fail(SbtError.InvalidCommand(s"sbt $command"))
                            else {
                              println(s"ERRRRRRR ${errorString}")
                              ZIO.fail(SbtError.SbtErrorMessage(errorString))
                            }
                          })
        } yield ZStream.mergeAllUnbounded()(
          ZStream.succeed(s"sbt $command"),
          process.stdout.linesStream,
          errorStream
        )
      )
      .catchSome { case SbtError.WaitingForLock => runSbtCommand(command) }
      .refineToOrDie[SbtError]

}
