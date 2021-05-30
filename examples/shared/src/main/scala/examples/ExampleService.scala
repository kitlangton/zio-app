package examples

import zio.UIO
import zio.stream._

case class Event(description: String, timestamp: Long)

trait ExampleService {
  def magicNumber: UIO[Int]
  def eventStream: Stream[Int, Event]
}
