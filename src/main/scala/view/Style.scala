package view

sealed abstract class Style(val code: String)

object Style {
  case object Bold       extends Style(scala.Console.BOLD)
  case object Underlined extends Style(scala.Console.UNDERLINED)
}
