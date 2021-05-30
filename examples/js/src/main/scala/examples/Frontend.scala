package examples

import com.raquo.airstream.split.Splittable
import com.raquo.laminar.api.L._
import org.scalajs.dom
import zio._
import zio.app.DeriveClient

object Frontend {
  def main(args: Array[String]): Unit = {
    val _ = documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app")
      appContainer.innerHTML = ""
      val _ = render(appContainer, view)
    }(unsafeWindowOwner)
  }

  lazy val runtime = zio.Runtime.default
  lazy val client  = DeriveClient.gen[ExampleService]

  val events: Var[Vector[String]] = Var(Vector.empty)

  def view: Div =
    div(
      onMountCallback { _ =>
        runtime.unsafeRunAsync_ {
          client.eventStream
            .foreach { event =>
              println(s"RECEIVEDD $event")
              UIO(events.update(_.appended(event.toString)))
            }
            .catchAll { error =>
              UIO(events.update(_.appended("ERROR: " + error)))
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
