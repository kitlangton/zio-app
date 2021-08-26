package zio.ui

import com.raquo.laminar.api.L._
import animus._

import java.util.UUID

object Styles {
  // Every element gets these styles. Sort of like a reset
  val styles = styleTag("""
.s {
  position: relative;
  border: none;
  flex-shrink: 0;
  display: flex;
  flex-direction: row;
  flex-basis: auto;
  resize: none;
  font-feature-settings: inherit;
  box-sizing: border-box;
  margin: 0;
  padding: 0;
  border-width: 0;
  border-style: solid;
  font-size: inherit;
  color: inherit;
  font-family: inherit;
  line-height: 1;
  font-weight: inherit;
  text-decoration: none;
  font-style: inherit;
}

.s.r {
  display: flex;
  flex-direction: row;
}
      """)
}

object Example {
  val $spacing: Signal[Int] =
    EventStream
      .periodic(2000)
      .toSignal(0)

  val view = ???
}

sealed trait View { self =>
//  def id: String

  def render: HtmlElement =
    div(
      width("100%"),
      height("auto"),
      minHeight("100%"),
      zIndex(0),
      Styles.styles,
      renderImpl
    )

  val rowStyles = List(
    display.flex,
    flexDirection.column
  )

  private def renderImpl: HtmlElement =
    self match {
      case View.Row(spacing, attributes, views) =>
        val $width = spacing.getOrElse(Val(0.0)).spring.px
        div(
          cls("s", "r"),
          children <-- views.splitTransition(_.hashCode()) { (key, view, $view, transition) =>
            div(
              transition.width,
              child <-- $view.map(_.render)
            )
          }
//          views.take(1).map(_.renderImpl),
//          views.drop(1).flatMap { view =>
//            List(
//              div(width <-- $width),
//              view.renderImpl
//            )
//          }
        )

      case View.WrappedRow(attributes, views) =>
        ???

      case View.Column(attributes, views) =>
        ???

      case View.Text(string) =>
        span(cls("s"), string)
    }
}

object View {
//  def row(spacing: Double)(views: View*): View =
//    Row(Some(Val(spacing)), List.empty, Val(views.toList))
//
//  def row(spacing: Signal[Double])(views: View*): View =
//    Row(Some(spacing), List.empty, Val(views.toList))
//
//  def row(views: View*): View = Row(None, List.empty, Val(views.toList))

  def foreach[A](values: Iterable[A], id: A => String)(view: A => View): View = ???
//    Row(None, List.empty, values.zipWithIndex.map { case (a, i) => view(a).id(i) })

  def row(views: Signal[List[View]]): View = Row(None, List.empty, views)

  def text(string: String): View = Text(string)

  final case class Row(
      spacing: Option[Signal[Double]],
      attributes: List[Attribute],
      views: Signal[List[View]]
  ) extends View

  final case class WrappedRow(attributes: List[Attribute], views: List[View]) extends View

  final case class Column(attributes: List[Attribute], views: List[View]) extends View

  final case class Text(string: String) extends View
}

sealed trait Attribute

object Attribute {
  final case class Width(value: Int)   extends Attribute
  final case class Height(value: Int)  extends Attribute
  final case class Spacing(value: Int) extends Attribute
  final case object CenterX            extends Attribute
}
