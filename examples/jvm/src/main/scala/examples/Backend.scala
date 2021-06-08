package examples

import zhttp.http._
import zhttp.service.Server
import zio._
import zio.app.DeriveRoutes
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.duration.durationInt
import zio.random.Random
import zio.stream._

case class Person(name: String, age: Int)
case class Dog(name: String, age: Int)

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

case class ExampleServiceLive(random: Random.Service, console: Console.Service, clock: Clock.Service)
    extends ExampleService {
  override def magicNumber: UIO[Int] =
    for {
      int <- random.nextInt
      _   <- console.putStrLn(s"GENERATED: $int").orDie
    } yield int

  val eventTypes = Vector(
    "POST",
    "PATCH",
    "MURDER",
    "MARRY",
    "INVERT",
    "REASSOCIATE",
    "DISASSOCIATE",
    "DEFACTOR"
  )

  val randomEventType: UIO[String] = random.shuffle(eventTypes).map(_.head)

  var i = -9999999
  val event = randomEventType
    .zipWith(random.nextInt)(Event(_, _))
    .filterOrFail(_.timestamp % 13 != 0)(9999)
    .debug("EVENT")

  override def eventStream: Stream[Int, Event] = {
    (ZStream.fromEffect(event) ++ ZStream.repeatEffect(event.delay(100.millis)))
      .provide(Has(clock))
  }

  override def attemptToProcess(event: Event): IO[String, Int] = {
    val int = event.timestamp.toInt
    if (int % 2 == 0) ZIO.fail(s"${int} WAS EVEN! UNACCEPTABLE")
    else UIO(int)
  }
}
