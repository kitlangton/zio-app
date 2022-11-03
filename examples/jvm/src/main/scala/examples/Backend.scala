package examples

import examples.services.{ExampleServiceLive, ParameterizedServiceLive}
import zhttp.http._
import zhttp.service.Server
import zio._
import zio.app.DeriveRoutes

case class Person(name: String, age: Int)
case class Dog(name: String, age: Int)

object Backend extends ZIOAppDefault {
  val httpApp: HttpApp[ExampleService with ParameterizedService[Int], Throwable] =
    DeriveRoutes.gen[ExampleService] ++ DeriveRoutes.gen[ParameterizedService[Int]]

  override def run = (for {
    port <- System.envOrElse("PORT", "8088").map(_.toInt).orElseSucceed(8088)
    _    <- Console.printLine(s"STARTING SERVER ON PORT $port")
    _    <- Server.start(port, httpApp)
  } yield ())
    .provide(
      ExampleServiceLive.layer,
      ParameterizedServiceLive.layer,
    )
}
