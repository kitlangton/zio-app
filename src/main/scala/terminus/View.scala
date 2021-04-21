package terminus

import terminus.View.{Color, Style, Styles, string2Text}
import StringSyntax._

import scala.util.Try

object StringSyntax {
  implicit class StringOps(val self: String) extends AnyVal {
    def removingAnsiCodes: String =
      self.replaceAll("\u001B\\[[;\\d]*m", "")
  }
}

sealed trait View { self =>
  def height: Int
  def width: Int
  def render(styles: Styles = Styles(None, None), frame: Int): String

  def ++(that: View): View        = View.HStack(Vector(self, that), 1)
  def colored(color: Color): View = View.Styled(self, Some(color), None)
  def red: View                   = View.Styled(self, Some(Color.Red), None)
  def blue: View                  = View.Styled(self, Some(Color.Blue), None)
  def yellow: View                = View.Styled(self, Some(Color.Yellow), None)
  def white: View                 = View.Styled(self, Some(Color.White), None)
  def bold: View                  = View.Styled(self, None, Some(Style.Bold))
  def underlined: View            = View.Styled(self, None, Some(Style.Underlined))
  def bordered: View              = View.Bordered(self)
}

object View {
  def text(string: String): View = View.Text(string)
  def hstack(views: View*): View = HStack(views.toVector, 1)
  def vstack(views: View*): View = VStack(views.toVector, 0)

  implicit def string2Text(string: String): View = View.Text(string)

  sealed abstract class Color(val code: String)

  case class Styles(colorOpt: Option[Color], styleOpt: Option[Style]) {
    def update(color: Option[Color], style: Option[Style]): Styles =
      copy(
        colorOpt = color.orElse(colorOpt),
        styleOpt = style.orElse(styleOpt)
      )

    def render: String =
      scala.Console.RESET + colorOpt.map(_.code).getOrElse("") + styleOpt.map(_.code).getOrElse("")
  }

  object Color {
    case object Red    extends Color(scala.Console.RED)
    case object Blue   extends Color(scala.Console.BLUE)
    case object Cyan   extends Color(scala.Console.CYAN)
    case object Yellow extends Color(scala.Console.YELLOW)
    case object White  extends Color(scala.Console.WHITE)
  }

  sealed abstract class Style(val code: String)

  object Style {
    case object Bold       extends Style(scala.Console.BOLD)
    case object Underlined extends Style(scala.Console.UNDERLINED)
  }

  case class Styled(view: View, color: Option[Color], style: Option[Style]) extends View {
    override def height: Int = view.height
    override def width: Int  = view.width
    override def render(styles: Styles, frame: Int): String = {
      val newStyles = styles.update(color, style)
      newStyles.render + view.render(newStyles, frame) + styles.render
    }
  }

  case class Bordered(view: View) extends View {
    override def height: Int = view.height + 2
    override def width: Int  = view.width + 4
    override def render(styles: Styles, frame: Int): String = {
      val vWidth = view.width
      val top    = "┌" + ("─" * (vWidth + 2)) + "┐"
      val bottom = "└" + ("─" * (vWidth + 2)) + "┘"
      val vRender = view
        .render(styles, frame)
        .linesIterator
        .map { line => "│ " + line + (" " * (vWidth - line.removingAnsiCodes.length)) + " │" }
        .mkString("\n")
      List(top, vRender, bottom).mkString("\n")
    }
  }

  case class Text(string: String) extends View {
    override def height: Int = 1
    override def width: Int  = string.length
    override def render(styles: Styles, frame: Int): String =
      string.take(frame)
  }

  case class HStack(views: Vector[View], padding: Int) extends View {
    lazy val height: Int = views.foldLeft(0)(_ max _.height)
    lazy val width: Int  = views.foldLeft(0)(_ + _.width) + ((views.length - 1) * padding)

    override def render(styles: Styles, frame: Int): String =
      if (views.isEmpty) {
        ""
      } else {
        val maxHeight = height

        def centerView(view: View): View = {
          val vDiff  = maxHeight - view.height
          val filler = List.fill(vDiff)(View.text(" " * view.width))
          val lines  = filler ++ List(view)
          vstack(lines: _*)
        }

        views.tail.map(centerView).foldLeft(centerView(views.head).render(styles, frame)) { (acc: String, view) =>
          acc.linesIterator
            .zip(view.render(styles, frame).linesIterator)
            .map { case (s1, s2) => s1 + " " + s2 }
            .mkString("\n")
        }
      }
  }

  case class VStack(views: Vector[View], padding: Int) extends View {
    lazy val height: Int = views.foldLeft(0)(_ + _.height)
    lazy val width: Int  = views.foldLeft(0)(_ max _.width)
    override def render(styles: Styles, frame: Int): String = {
      if (views.isEmpty) ""
      else views.tail.foldLeft(views.head.render(styles, frame))(_ + "\n" + _.render(styles, frame))
    }
  }
}

/** example:
  * SwiftUI
  *
  * Frame
  * Text
  * Styling
  *
  * text("hello").center
  *
  *  ------------
  *  |           |
  *  |   hello   |
  *  |           |
  *  -------------
  *
  *  (text("A") ++ text("B") ++ text("C")).aligned(Alignment.bottomRight)
  *
  *  -------------
  *  |ABC        |
  *  |           |
  *  |           |
  *  -------------
  *
  *  (text("A").center.width(8) ++ text("B")).aligned(Alignment.bottomRight)
  *
  *  -------------
  *  |A       B  |
  *  |           |
  *  |           |
  *  -------------
  *
  *  val top =
  *  val backend =
  *  val frontend =
  *
  *  val view =
  *     vert(
  *       top
  *         .centerH
  *         .padding(1),
  *       horiz(
  *        backend.alignment(bottomLeft),
  *        frontend.alignment(bottomLeft))
  *     )
  *
  *  -------------
  *  |  zio-app  |
  *  |-----------|
  *  |b    |     |
  *  |b    |     |
  *  |be   |fe   |
  *  -------------
  *
  *  horiz(
  *     text("A").centerH,
  *     text("B").width(1),
  *     text("C").width(1))
  *     // .spacing(spaceBetween)
  *
  *  --------------
  *  |    A     BC|
  *  |            |
  *  |------------|
  *
  *  text("A").center ++ text("B").center
  *
  *  --------------
  *  |            |
  *  |  A      B  |
  *  |            |
  *  |------------|
  */

object Examples {
  def main(args: Array[String]): Unit = {
    val view =
      View
        .hstack(
          "hello".blue,
          "therein".red
        )

    println(view.render(frame = 100))
  }
}
