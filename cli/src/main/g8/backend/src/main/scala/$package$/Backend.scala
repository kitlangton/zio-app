package $package$

import $package$.protocol.{ExampleService}
import zio._
import zio.app.DeriveRoutes
import zio.console._
import zio.magic._

object Backend extends App {
  private val httpApp =
    DeriveRoutes.gen[ExampleService]

  val program = for {
    port <- system.envOrElse("PORT", "8088").map(_.toInt).orElseSucceed(8088)
    _    <- putStrLn(s"STARTING SERVER ON PORT $port")
    _    <- zhttp.service.Server.start(port, httpApp)
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    program.injectCustom(ExampleServiceLive.layer).exitCode
  }
}

case class ExampleServiceLive(random: zio.random.Random) extends ExampleService {
  override def magicNumber: UIO[Int] = random.nextInt
}

object ExampleServiceLive {
  val layer = ExampleServiceLive.toLayer[ExampleService]
}