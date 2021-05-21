package $package$

import com.raquo.laminar.api.L._
import $package$.protocol.{ExampleService}
import zio._
import zio.app.DeriveClient
import animus._

object Frontend {
  val runtime = Runtime.default
  val client  = DeriveClient.gen[ExampleService]

  def view: Div =
    div(
      h3("IMPORTANT WEBSITE"),
      debugView("MAGIC NUMBER", client.magicNumber)
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
}
