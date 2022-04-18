package examples.services

import examples.ParameterizedService
import examples.ParameterizedService.{Foo, FooId}
import zio._

case class ParameterizedServiceLive(ref: Ref[Map[FooId[Int], Foo[Int]]]) extends ParameterizedService[Int] {
  import examples.ParameterizedService._

  override def getAll: Task[List[Foo[Int]]] =
    ref.get.map(_.values.toList)

  override def create(m: CreateFoo): Task[Unit] = {
    val fooId  = FooId(scala.util.Random.nextInt)
    val newFoo = Foo(fooId, m.name, m.content, m.tags)
    ref.update(_.updated(fooId, newFoo))
  }

  override def update(m: UpdateFoo[Int]): Task[Unit] =
    ref.update(_.updated(m.id, Foo(m.id, m.name, m.content, m.tags)))

  override def delete(id: FooId[Int]): Task[Unit] =
    ref.update(_.removed(id))
}

object ParameterizedServiceLive {
  val layer: ULayer[ParameterizedService[Int]] =
    ZLayer {
      Ref
        .make(Map.empty[FooId[Int], Foo[Int]])
        .map { ref =>
          ParameterizedServiceLive(ref)
        }
    }
}
