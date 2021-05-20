package view

case class Size private (width: Int, height: Int) { self =>
  def scaled(dx: Int, dy: Int): Size =
    Size(width + dx, height + dy)

  def overriding(width: Option[Int] = None, height: Option[Int] = None): Size =
    Size(width.getOrElse(self.width), height.getOrElse(self.height))
}

object Size {
  def apply(width: Int, height: Int) =
    new Size(width max 0, height max 0)
}
