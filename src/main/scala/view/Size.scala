package view

case class Size(width: Int, height: Int) { self =>

  def scaled(dx: Int, dy: Int): Size = Size((width + dx) max 0, (height + dy) max 0)

  def overriding(width: Option[Int] = None, height: Option[Int] = None): Size =
    Size(width.getOrElse(self.width), height.getOrElse(self.height))
}
