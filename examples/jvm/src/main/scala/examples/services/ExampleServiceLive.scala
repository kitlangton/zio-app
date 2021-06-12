package examples.services

import examples.{Event, ExampleService}
import zio.clock.Clock
import zio.console.Console
import zio.random.Random
import zio.stream.{Stream, ZStream}
import zio._
import zio.duration._

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
    if (int % 2 == 0) ZIO.fail(s"$int WAS EVEN! UNACCEPTABLE")
    else UIO(int)
  }

  override def unit: UIO[Unit] = UIO.unit
}

object ExampleServiceLive {

  val layer: URLayer[Clock with Random with Console, Has[ExampleService]] = (ExampleServiceLive.apply _).toLayer

}
