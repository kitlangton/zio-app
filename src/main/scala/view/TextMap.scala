package view

class RenderContext(val textMap: TextMap, var x: Int, var y: Int) {

  def align(childSize: Size, parentSize: Size, alignment: Alignment): Unit = {
    val parentPoint = alignment.point(parentSize)
    val childPoint  = alignment.point(childSize)
    translateBy(parentPoint.x - childPoint.x, parentPoint.y - childPoint.y)
  }

  def insert(string: String): Unit =
    textMap.insert(string, x, y)

  def translateBy(dx: Int, dy: Int): Unit = {
    x += dx
    y += dy
  }

  def scratch(f: => Unit): Unit = {
    val x0 = x
    val y0 = y
    f
    x = x0
    y = y0
  }
}

class TextMap(map: Array[Array[String]], width: Int, height: Int) {
  def apply(x: Int, y: Int): String =
    if (0 <= x && x < width && 0 <= y && y < height)
      map(y)(x)
    else ""

  def update(x: Int, y: Int, string: String): Unit =
    if (0 <= x && x < width && 0 <= y && y < height)
      map(y)(x) = string

  def add(char: Char, x: Int, y: Int): Unit = {
    if (0 <= x && x < width && 0 <= y && y < height)
      map(y)(x) = char.toString
  }

  def insert(string: String, x: Int, y: Int): Unit = {
    var currentX = x
    string.foreach { char =>
      add(char, currentX, y)
      currentX += 1
    }
  }

  override def toString: String =
    map.map { _.mkString("") }.mkString("\n")
}

object TextMap {
  def ofDim(width: Int, height: Int, empty: String = " "): TextMap =
    new TextMap(Array.fill(height, width)(empty), width, height)
}
