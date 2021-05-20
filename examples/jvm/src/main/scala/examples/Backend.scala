package examples

import zhttp.http.HttpApp
import zhttp.service.Server
import zio._
import zio.app.DeriveRoutes
import zio.console.{Console, putStrLn}
import zio.random.Random

object Backend extends App {
  val httpApp: HttpApp[Has[ExampleService], Throwable] =
    DeriveRoutes.gen[ExampleService]

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = (for {
    port <- system.envOrElse("PORT", "8088").map(_.toInt).orElseSucceed(8088)
    _    <- putStrLn(s"STARTING SERVER ON PORT $port")
    _    <- Server.start(port, httpApp)
  } yield ())
    .provideCustomLayer(ExampleServiceLive.toLayer[ExampleService])
    .exitCode
}

case class ExampleServiceLive(random: Random.Service, console: Console.Service) extends ExampleService {
  override def magicNumber: UIO[Int] =
    for {
      int <- random.nextInt
      _   <- console.putStrLn(s"GENERATED: $int").orDie
    } yield int
}
