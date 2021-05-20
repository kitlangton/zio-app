package examples

import zio._
import com.raquo.laminar.api.L._
import org.scalajs.dom
import zio.app.App

object Frontend {
  def main(args: Array[String]): Unit = {
    val _ = documentEvents.onDomContentLoaded.foreach { _ =>
      val appContainer = dom.document.querySelector("#app")
      appContainer.innerHTML = ""
      val _ = render(appContainer, view)
    }(unsafeWindowOwner)
  }

  lazy val runtime = zio.Runtime.default
  lazy val client  = App.client[ExampleService]

  def view: Div =
    debugView("Magic Number", client.magicNumber)

  def debugView[A](name: String, effect: => UIO[A]): Div = {
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
}
