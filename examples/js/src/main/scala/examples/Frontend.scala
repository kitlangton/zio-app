package examples

import com.raquo.airstream.split.Splittable
import com.raquo.laminar.api.L._
import org.scalajs.dom
import zio._
import zio.app.DeriveClient
import zio.duration.durationInt
import zio.stream.ZStream

object Frontend {
  def main(args: Array[String]): Unit = {
    val _ = documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app")
      appContainer.innerHTML = ""
      val _ = render(appContainer, view)
    }(unsafeWindowOwner)
  }

  val runtime                            = zio.Runtime.default
  val client: ExampleService             = DeriveClient.gen[ExampleService]
  val client2: ParameterizedService[Int] = DeriveClient.gen[ParameterizedService[Int]]

  val events: Var[Vector[String]] = Var(Vector.empty)

  def view: Div =
    div(
      onMountCallback { _ =>
        runtime.unsafeRunAsync_ {
          client.eventStream
            .retry(Schedule.spaced(1.second))
            .foreach { event =>
              println(s"RECEIVED: $event")
              UIO(events.update(_.appended(event.toString)))
            }
        }
      },
      debugView("Magic Number", client.magicNumber),
      children <-- events.signal.map(_.zipWithIndex.reverse).split(_._2) { (_, event, _) =>
        div(event._1)
      }
    )

  private def debugView[A](name: String, effect: => UIO[A]): Div = {
    val output = Var(List.empty[String])
    div(
      h3(name),
      children <-- output.signal.map { strings =>
        strings.map(div(_))
      },
      onClick --> { _ =>
        runtime.unsafeRunAsync_ {
          effect.tap { a => UIO(output.update(_.prepended(a.toString))) }
        }
      }
    )
  }

  implicit val chunkSplittable: Splittable[Chunk] = new Splittable[Chunk] {
    override def map[A, B](inputs: Chunk[A], project: A => B): Chunk[B] = inputs.map(project)
  }
}
