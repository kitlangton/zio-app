package examples

import zio.{IO, UIO}
import zio.stream._

case class Event(description: String, timestamp: Long)

trait ExampleService {
  def magicNumber: UIO[Int]
  def attemptToProcess(event: Event): IO[String, Int]
  def eventStream: Stream[Int, Event]
}
