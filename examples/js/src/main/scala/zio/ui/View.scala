package zio.ui

import animus._
import com.raquo.laminar.api.L._

object Styles {
  // Every element gets these styles. Sort of like a reset
  val styles = styleTag("""
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@100;200;300;400;500;600;700;800;900&display=swap');

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

.s.c {
  display: flex;
  flex-direction: column;
}
      """)
}

object Example {
  val $spacing: Signal[Int] =
    EventStream
      .periodic(2000)
      .toSignal(0)

  import View._

  val view =
    row(24)(
      col(12)(
//        Color.red,
        text("ZIO"),
        text("HACK-"),
        text("A-"),
        text("THON")
      ),
      text("COOL"),
      row(12)(
        Color.blue,
        text("hello"),
        text("NICE"),
        text("Very cool")
      )
    )
}

sealed trait View extends Modifier { self =>

  def render: HtmlElement =
    div(
      fontFamily("Inter"),
      fontWeight.bolder,
      fontSize("16px"),
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
      case View.Stack(direction, spacing, styles, views) =>
        renderStack(direction, spacing, styles, views)

      case View.WrappedRow(_, _) =>
        ???

      case View.Text(string) =>
        div(cls("s"), string)
    }

  private def renderStack(
      direction: Direction,
      spacing: Option[Signal[Double]],
      styles: List[Style],
      views: Signal[List[View]]
  ): Div = {
    val $width = spacing.getOrElse(Val(0.0)).spring.px
    div(
      cls("s", direction.className),
      children <-- views.map(_.zipWithIndex).splitTransition(_._1.hashCode()) { (_, _, $view, transition) =>
        div(
          styles.map(_.toMod),
          child <-- $view.map { case (view, idx) =>
            val rendered = view.renderImpl
            if (idx > 0) rendered.amend(direction.spacingStyle <-- $width)
            else rendered
          },
          transition.width
        )
      }
    )
  }
}

sealed trait Direction { self =>
  def className: String = self match {
    case Direction.Horizontal => "r"
    case Direction.Vertical   => "c"
  }

  def spacingStyle = self match {
    case Direction.Horizontal => marginLeft
    case Direction.Vertical   => marginTop
  }
}

object Direction {
  case object Horizontal extends Direction

  case object Vertical extends Direction
}

object View {
  def col(views: View*): View =
    Stack(Direction.Vertical, None, List.empty, Val(views.toList))

  def col(spacing: Double)(views: View*): View =
    Stack(Direction.Vertical, Some(Val(spacing)), List.empty, Val(views.toList))

  def row(spacing: Double)(modifiers: Modifier*): View = {
    Stack(
      Direction.Horizontal,
      Some(Val(spacing)),
      modifiers.collect { case s: Style => s }.toList,
      Val(modifiers.toList.collect { case v: View => v })
    )
  }

  def row(spacing: Signal[Double])(views: View*): View =
    Stack(Direction.Horizontal, Some(spacing), List.empty, Val(views.toList))

  def row(views: View*): View = Stack(Direction.Horizontal, None, List.empty, Val(views.toList))

  def foreach[A](values: Iterable[A], id: A => String)(view: A => View): View = ???

  def row(views: Signal[List[View]]): View = Stack(Direction.Horizontal, None, List.empty, views)

  def text(string: String): View = Text(string)

  final case class Stack(
      direction: Direction,
      spacing: Option[Signal[Double]],
      styles: List[Style],
      views: Signal[List[View]]
  ) extends View

  final case class WrappedRow(attributes: List[Attribute], views: List[View]) extends View

  final case class Text(string: String) extends View
}

sealed trait Modifier

sealed trait Style extends Modifier {
  def toMod: Mod[HtmlElement]

}

final case class Color(value: String) extends Style {
  override def toMod: Mod[HtmlElement] = color(value)
}
object Color {
  val red   = Color("red")
  val blue  = Color("blue")
  val black = Color("black")
}

sealed trait Attribute

object Attribute {
  final case class Width(value: Int)   extends Attribute
  final case class Height(value: Int)  extends Attribute
  final case class Spacing(value: Int) extends Attribute
  final case object CenterX            extends Attribute
}
