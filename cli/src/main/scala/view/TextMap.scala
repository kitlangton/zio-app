package view

import tui.StringSyntax.StringOps

class RenderContext(val textMap: TextMap, var x: Int, var y: Int) {

  def align(childSize: Size, parentSize: Size, alignment: Alignment): Unit = {
    val parentPoint = alignment.point(parentSize)
    val childPoint  = alignment.point(childSize)
    translateBy(parentPoint.x - childPoint.x, parentPoint.y - childPoint.y)
  }

  def insert(string: String, color: Color = Color.Default, style: Style = Style.Default): Unit =
    textMap.insert(string, x, y, color, style)

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

class TextMap(
    text: Array[Array[String]],
    colors: Array[Array[Color]],
    styles: Array[Array[Style]],
    private val width: Int,
    private val height: Int
) { self =>

  def apply(x: Int, y: Int): String =
    if (0 <= x && x < width && 0 <= y && y < height)
      text(y)(x)
    else ""

  def setColor(x: Int, y: Int, color: Color): Unit =
    if (0 <= x && x < width && 0 <= y && y < height)
      colors(y)(x) = color

  def setStyle(x: Int, y: Int, style: Style): Unit =
    if (0 <= x && x < width && 0 <= y && y < height)
      styles(y)(x) = style

  def getColor(x: Int, y: Int): Color =
    if (0 <= x && x < width && 0 <= y && y < height)
      colors(y)(x)
    else Color.Default

  def getStyle(x: Int, y: Int): Style =
    if (0 <= x && x < width && 0 <= y && y < height)
      styles(y)(x)
    else Style.Default

  def update(x: Int, y: Int, string: String): Unit =
    if (0 <= x && x < width && 0 <= y && y < height)
      text(y)(x) = string

  def add(char: Char, x: Int, y: Int, color: Color = Color.Default, style: Style = Style.Default): Unit = {
    self(x, y) = char.toString
    setColor(x, y, color)
    setStyle(x, y, style)
  }

  def insert(string: String, x: Int, y: Int, color: Color = Color.Default, style: Style = Style.Default): Unit = {
    var currentX = x
    string.foreach { char =>
      add(char, currentX, y, color, style)
      currentX += 1
    }
  }

  override def toString: String = {
    val builder      = new StringBuilder()
    var color: Color = Color.Default
    var style: Style = Style.Default
    var y            = 0
    text.foreach { line =>
      var x = 0
      line.foreach { char =>
        val newColor = colors(y)(x)
        val newStyle = styles(y)(x)

        // TODO: Clean up this nonsense. Actually model the terminal styling domain.
        if (newColor != color || newStyle != style) {
          color = newColor
          style = newStyle
          builder.addAll(scala.Console.RESET)
          builder.addAll(color.code)
          builder.addAll(style.code)
        }
        builder.addAll(char)
        x += 1
      }
      if (y < height - 1) {
        y += 1
        builder.addOne('\n')
      }
    }
    builder.toString()
  }
}

object TextMap {
  def ofDim(width: Int, height: Int, empty: String = " "): TextMap =
    new TextMap(
      Array.fill(height, width)(empty),
      Array.fill(height, width)(Color.Default),
      Array.fill(height, width)(Style.Default),
      width,
      height
    )

  def diff(oldMap: TextMap, newMap: TextMap, width: Int, height: Int): String = {
    val result = new StringBuilder()
    result.addAll(moveCursor(0, 0))
    for (x <- 0 until width; y <- 0 until height) {
      val oldChar  = oldMap(x, y)
      val newChar  = newMap(x, y)
      val oldColor = oldMap.getColor(x, y)
      val newColor = newMap.getColor(x, y)
      val oldStyle = oldMap.getStyle(x, y)
      val newStyle = newMap.getStyle(x, y)

      if (oldChar != newChar || oldColor != newColor || oldStyle != newStyle) {
        result.addAll(moveCursor(x, y))
        result.addAll(newColor.code)
        result.addAll(newStyle.code)
        result.addAll(newChar)
        result.addAll(scala.Console.RESET)
      }

    }

    result.addAll(moveCursor(width, height) + scala.Console.RESET)
    result.toString()
  }

  private def moveCursor(x: Int, y: Int): String = s"\u001b[${y + 1};${x + 1}H"

  def main(args: Array[String]): Unit = {
    val oldMap = TextMap.ofDim(8, 8)
    val newMap = TextMap.ofDim(8, 8)
    oldMap.insert("cool", 4, 4)
    newMap.insert("cool", 2, 4)
    val result = diff(oldMap, newMap, 8, 8)
    val start  = diff(TextMap.ofDim(0, 0), oldMap, 8, 8)
    println(start)
    println(result)
  }
}
