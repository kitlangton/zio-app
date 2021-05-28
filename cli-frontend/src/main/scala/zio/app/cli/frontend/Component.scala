package zio.app.cli.frontend

import com.raquo.laminar.api.L._

import scala.language.implicitConversions

trait Component {
  def body: HtmlElement
}

object Component {
  implicit def toLaminarElement(component: Component): HtmlElement = component.body
}
