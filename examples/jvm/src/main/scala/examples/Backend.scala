package examples

import boopickle.BufferPool
import boopickle.Default._
import zhttp.http.HttpApp
import zhttp.service.Server
import zio._
import zio.app.DeriveRoutes
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.duration.durationInt
import zio.random.Random
import zio.stream.{UStream, ZStream}

case class Person(name: String, age: Int)
case class Dog(name: String, age: Int)

object Backend extends App {
  val httpApp: HttpApp[Has[ExampleService], Throwable] =
    DeriveRoutes.gen[ExampleService]

  BufferPool.disable()

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
//    "REFACTOR",
    "DEFACTOR"
  )

  val randomEventType: UIO[String] = random.shuffle(eventTypes).map(_.head)

  var i     = -9999999
  val event = randomEventType.zipWith(random.nextInt)(Event(_, _))

  override def eventStream: UStream[Event] = {
    (ZStream.fromEffect(event) ++ ZStream.repeatEffect(event.delay(3000.millis)))
      .provide(Has(clock))
      .tap { i =>
        UIO(println(i))
      }
  }
}

//object StreamExample extends App {
//  var i = 0
//  val value: UIO[Event] = UIO({
//    i += 1; Event("COOL", i)
//  })
//  val program: ZIO[Clock, Nothing, Unit] =
//    (ZStream.fromEffect(value) ++
//      ZStream.repeatEffect(value.delay(4000.millis)))
//      .mapConcatChunk {  i =>
//        Chunk.fromByteBuffer(Pickle.intoBytes(i))
//      }
//      .foreachChunk { int =>
//        UIO(println(s"INT: $int"))
//      }
//
//  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
//    (UIO(println("START")) *> program).exitCode
//  }
//}
