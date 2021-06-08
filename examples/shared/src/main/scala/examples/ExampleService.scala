package examples

import examples.ParameterizedService.{CreateFoo, FooId, UpdateFoo}
import zio.{IO, Task, UIO}
import zio.stream._

case class Event(description: String, timestamp: Long)

trait ExampleService {
  def magicNumber: UIO[Int]
  def attemptToProcess(event: Event): IO[String, Int]
  def eventStream: Stream[Int, Event]
}

trait ParameterizedService[T] {
  def getAll: Task[List[ParameterizedService.Foo[T]]]
  def create(m: CreateFoo): Task[Unit]
  def update(m: UpdateFoo[T]): Task[Unit]
  def delete(id: FooId[T]): Task[Unit]
}

object ParameterizedService {
  final case class Foo[T](
      id: FooId[T],
      name: String,
      content: String,
      tags: List[String]
  )

  final case class CreateFoo(
      name: String,
      content: String,
      tags: List[String]
  )

  final case class UpdateFoo[T](
      id: FooId[T],
      name: String,
      content: String,
      tags: List[String]
  )

  final case class FooId[T](
      value: T
  )
}
