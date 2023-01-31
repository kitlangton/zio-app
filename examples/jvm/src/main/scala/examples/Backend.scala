package examples

import examples.services.{ExampleServiceLive, ParameterizedServiceLive}
import zio.http._
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
    _ <- Server
           .serve(httpApp)
           .provideSome[ExampleService with ParameterizedService[Int]](
             ServerConfig.live(ServerConfig.default.port(port)),
             Server.live
           )
  } yield ())
    .provide(
      ExampleServiceLive.layer,
      ParameterizedServiceLive.layer
    )
}
