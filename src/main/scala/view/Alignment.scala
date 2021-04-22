package view

case class Coord(x: Int, y: Int)

case class Alignment(horizontalAlignment: HorizontalAlignment, verticalAlignment: VerticalAlignment) {

  def point(size: Size): Coord =
    Coord(
      horizontalAlignment.point(size.width),
      verticalAlignment.point(size.height)
    )
}

object Alignment {
  val top: Alignment         = Alignment(HorizontalAlignment.Center, VerticalAlignment.Top)
  val bottom: Alignment      = Alignment(HorizontalAlignment.Center, VerticalAlignment.Bottom)
  val center: Alignment      = Alignment(HorizontalAlignment.Center, VerticalAlignment.Center)
  val left: Alignment        = Alignment(HorizontalAlignment.Left, VerticalAlignment.Center)
  val right: Alignment       = Alignment(HorizontalAlignment.Right, VerticalAlignment.Center)
  val topLeft: Alignment     = Alignment(HorizontalAlignment.Left, VerticalAlignment.Top)
  val topRight: Alignment    = Alignment(HorizontalAlignment.Right, VerticalAlignment.Top)
  val bottomLeft: Alignment  = Alignment(HorizontalAlignment.Left, VerticalAlignment.Bottom)
  val bottomRight: Alignment = Alignment(HorizontalAlignment.Right, VerticalAlignment.Bottom)
}

sealed trait HorizontalAlignment { self =>
  def point(width: Int): Int = self match {
    case HorizontalAlignment.Left   => 0
    case HorizontalAlignment.Center => width / 2
    case HorizontalAlignment.Right  => width
  }
}

object HorizontalAlignment {
  case object Left   extends HorizontalAlignment
  case object Center extends HorizontalAlignment
  case object Right  extends HorizontalAlignment
}

sealed trait VerticalAlignment { self =>
  def point(height: Int): Int = self match {
    case VerticalAlignment.Top    => 0
    case VerticalAlignment.Center => height / 2
    case VerticalAlignment.Bottom => height
  }
}

object VerticalAlignment {
  case object Top    extends VerticalAlignment
  case object Center extends VerticalAlignment
  case object Bottom extends VerticalAlignment
}
