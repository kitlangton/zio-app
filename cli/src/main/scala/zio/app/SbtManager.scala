package zio.app

import zio.{Chunk, Has, URLayer}
import zio.app.DevMode.backendLines
import zio.blocking.Blocking
import zio.stream._
import zio._

trait SbtManager {
  def backendSbtStream: Stream[Throwable, Chunk[String]]
}

object SbtManager {
  val live: URLayer[Blocking, Has[SbtManager]] =
    SbtManagerLive.toLayer[SbtManager]

  val backendSbtStream: ZStream[Has[SbtManager], Throwable, Chunk[String]] =
    ZStream.accessStream[Has[SbtManager]](_.get.backendSbtStream)
}

case class SbtManagerLive(blocking: zio.blocking.Blocking.Service) extends SbtManager {
  override def backendSbtStream: Stream[Throwable, Chunk[String]] =
    backendLines
      .tap { lines =>
        UIO(println(lines))
      }
      .provide(Has(blocking))
}
