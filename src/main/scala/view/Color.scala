package view

sealed abstract class Color(val code: String)

object Color {
  case object Red    extends Color(scala.Console.RED)
  case object Blue   extends Color(scala.Console.BLUE)
  case object Cyan   extends Color(scala.Console.CYAN)
  case object Yellow extends Color(scala.Console.YELLOW)
  case object White  extends Color(scala.Console.WHITE)
}
