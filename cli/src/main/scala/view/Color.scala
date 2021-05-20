package view

sealed abstract class Color(val code: String)

object Color {
  case object Black   extends Color(scala.Console.BLACK)
  case object Blue    extends Color(scala.Console.BLUE)
  case object Cyan    extends Color(scala.Console.CYAN)
  case object Green   extends Color(scala.Console.GREEN)
  case object Magenta extends Color(scala.Console.MAGENTA)
  case object Red     extends Color(scala.Console.RED)
  case object White   extends Color(scala.Console.WHITE)
  case object Yellow  extends Color(scala.Console.YELLOW)
  case object Default extends Color("")
}
