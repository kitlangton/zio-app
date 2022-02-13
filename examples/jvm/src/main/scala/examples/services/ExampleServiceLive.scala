package examples.services

import examples.{Event, ExampleService}
import zio._
import zio.stream.{Stream, ZStream}

case class ExampleServiceLive(random: Random, console: Console, clock: Clock) extends ExampleService {
  override def magicNumber: UIO[Int] =
    for {
      int <- random.nextInt
      _   <- console.printLine(s"GENERATED: $int").orDie
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
    (ZStream.fromZIO(event) ++ ZStream.repeatZIO(event.delay(100.millis)))
      .provideService(clock)
  }

  override def attemptToProcess(event: Event): IO[String, Int] = {
    val int = event.timestamp.toInt
    if (int % 2 == 0) ZIO.fail(s"$int WAS EVEN! UNACCEPTABLE")
    else UIO(int)
  }

  override def unit: UIO[Unit] = UIO.unit
}

object ExampleServiceLive {

  val layer: URLayer[Clock with Random with Console, ExampleService] = (ExampleServiceLive.apply _).toLayer

}
