package view

sealed abstract class Style(val code: String)

object Style {
  case object Bold       extends Style(scala.Console.BOLD)
  case object Underlined extends Style(scala.Console.UNDERLINED)
  case object Reversed   extends Style(scala.Console.REVERSED)
  case object Dim        extends Style("\u001b[2m")
  case object Default    extends Style("")
}
