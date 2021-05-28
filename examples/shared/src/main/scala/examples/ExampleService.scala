package examples

import zio.UIO
import zio.stream.UStream

case class Event(description: String, timestamp: Long)

trait ExampleService {
  def magicNumber: UIO[Int]
  def eventStream: UStream[Event]
}
