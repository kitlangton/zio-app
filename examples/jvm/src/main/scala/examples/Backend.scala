package examples

import examples.services.{ExampleServiceLive, ParameterizedServiceLive}
import zhttp.http._
import zhttp.service.Server
import zio._
import zio.app.DeriveRoutes
import zio.console.putStrLn
import zio.magic._

case class Person(name: String, age: Int)
case class Dog(name: String, age: Int)

object Backend extends App {
  val httpApp: HttpApp[Has[ExampleService] with Has[ParameterizedService[Int]], Throwable] =
    DeriveRoutes.gen[ExampleService] +++ DeriveRoutes.gen[ParameterizedService[Int]]

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = (for {
    port <- system.envOrElse("PORT", "8088").map(_.toInt).orElseSucceed(8088)
    _    <- putStrLn(s"STARTING SERVER ON PORT $port")
    _    <- Server.start(port, httpApp)
  } yield ())
    .injectCustom(
      ExampleServiceLive.layer,
      ParameterizedServiceLive.layer
    )
    .exitCode
}
