package view

import zio.Chunk

import scala.language.implicitConversions

/** - Brick
  * Elm
  */
sealed trait View { self =>

  def renderNow: String = {
    val termSize = Input.terminalSize
    val size     = self.size(Size(termSize._1, termSize._2))
    render(size.width, size.height)
  }

  def bordered: View = View.Border(self)

  def center: View =
    flex(maxWidth = Some(Int.MaxValue), maxHeight = Some(Int.MaxValue))

  def centerH: View =
    flex(maxWidth = Some(Int.MaxValue))

  def right: View =
    flex(maxWidth = Some(Int.MaxValue), alignment = Alignment.right)

  def left: View =
    flex(maxWidth = Some(Int.MaxValue), alignment = Alignment.left)

  def bottomLeft: View =
    flex(maxWidth = Some(Int.MaxValue), maxHeight = Some(Int.MaxValue), alignment = Alignment.bottomLeft)

  def bottomRight: View =
    flex(maxWidth = Some(Int.MaxValue), maxHeight = Some(Int.MaxValue), alignment = Alignment.bottomRight)

  def centerV: View =
    flex(maxHeight = Some(Int.MaxValue))

  def padding(amount: Int): View = padding(horizontal = amount, vertical = amount)

  def padding(horizontal: Int, vertical: Int): View =
    View.Padding(self, horizontal, vertical)

  def overlay(view: View, alignment: Alignment = Alignment.center): View = View.Overlay(self, view, alignment)

  def frame(width: Int, height: Int, alignment: Alignment = Alignment.center): View =
    View.FixedFrame(self, Some(width), Some(height), alignment)

  def flex(
      minWidth: Option[Int] = None,
      maxWidth: Option[Int] = None,
      minHeight: Option[Int] = None,
      maxHeight: Option[Int] = None,
      alignment: Alignment = Alignment.center
  ): View =
    View.FlexibleFrame(self, minWidth, maxWidth, minHeight, maxHeight, alignment)

  def size(proposed: Size): Size

  def render(context: RenderContext, size: Size): Unit

  def render(width: Int, height: Int): String = {
    val context = new RenderContext(TextMap.ofDim(width, height), 0, 0)
    self.render(context, Size(width, height))
    context.textMap.toString
  }

}

object View {
  def text(string: String): View = View.Text(
    string
      .replaceAll("\\n", "")
      .replaceAll("\\t", "  "),
    None
  )
  def text(string: String, color: Color): View = View.Text(
    string
      .replaceAll("\\n", "")
      .replaceAll("\\t", "  "),
    Some(color)
  )

  def horizontal(views: View*): View =
    View.Horizontal(Chunk.fromIterable(views))

  def vertical(views: View*): View =
    View.Vertical(Chunk.fromIterable(views), alignment = HorizontalAlignment.Left)

  implicit def string2View(string: String): View = text(string)

  case class Padding(view: View, horizontal: Int, vertical: Int) extends View {
    override def size(proposed: Size): Size = {
      view.size(proposed.scaled(horizontal * -2, vertical * -2)).scaled(horizontal * 2, vertical * 2)
    }

    override def render(context: RenderContext, size: Size): Unit = {
      val childSize = view.size(size.scaled(horizontal * -2, vertical * -2))
      context.scratch {
        context.align(childSize, size, Alignment.center)
        view.render(context, childSize)
      }
    }
  }

  case class Horizontal(views: Chunk[View], spacing: Int = 0, alignment: VerticalAlignment = VerticalAlignment.Center)
      extends View {
    override def size(proposed: Size): Size = {
      val sizes = layout(proposed)
      Size(sizes.map(_.width).sum, sizes.map(_.height).maxOption.getOrElse(0))
    }

    override def render(context: RenderContext, size: Size): Unit = {
      val selfY    = alignment.point(size.height)
      val sizes    = layout(size)
      var currentX = 0
      views.zipWith(sizes) { (view, childSize) =>
        context.scratch {
          val childY = alignment.point(childSize.height)
          context.translateBy(currentX, selfY - childY)
          view.render(context, childSize)
        }
        currentX += childSize.width + spacing
      }
    }

    private def layout(proposed: Size): Chunk[Size] = {
      val sizes: Array[Size] = Array.ofDim(views.length)

      val viewsWithFlex = views.zipWithIndex
        .map { case (view, idx) =>
          val lower = view.size(Size(0, proposed.height)).width
          val upper = view.size(Size(Int.MaxValue, proposed.height)).width
          (idx, view, upper - lower)
        }
        .sortBy(_._3)

      val total     = views.length
      var remaining = proposed.width
      var idx       = 0

      viewsWithFlex.foreach { case (i, view, _) =>
        val width     = remaining / (total - idx)
        val childSize = view.size(Size(width, proposed.height))
        idx += 1
        remaining -= childSize.width
        sizes(i) = childSize
      }

      val result = Chunk.fromArray(sizes)
      result
    }

  }

  case class Vertical(views: Chunk[View], spacing: Int = 0, alignment: HorizontalAlignment = HorizontalAlignment.Center)
      extends View {
    override def size(proposed: Size): Size = {
      val sizes  = layout(proposed)
      val result = Size(sizes.map(_.width).maxOption.getOrElse(0), sizes.map(_.height).sum)
      result
    }

    override def render(context: RenderContext, size: Size): Unit = {
      val sizes    = layout(size)
      var currentY = 0
      views.zipWith(sizes) { (view, childSize) =>
        context.scratch {
          context.translateBy(0, currentY)
          context.align(childSize, Size(size.width, childSize.height), Alignment(alignment, VerticalAlignment.Center))
          view.render(context, childSize)
        }
        currentY += childSize.height + spacing
      }
    }

    private def layout(proposed: Size): Chunk[Size] = {
      val total     = views.length
      var remaining = proposed.height - (spacing * (total - 1))
      var idx       = 0
      val sizes = views.flatMap { view =>
        if (remaining <= 0) None
        else {
          val childSize = view.size(Size(proposed.width, remaining / (total - idx)))
          idx += 1
          remaining -= childSize.height
          Some(childSize)
        }
      }
      sizes
    }

  }

  case class Text(string: String, color: Option[Color]) extends View {
    lazy val length: Int = string.length

    override def size(proposed: Size): Size =
      Size(width = string.length min proposed.width, height = 1)

    override def render(context: RenderContext, size: Size): Unit = {
      val taken = string.take(size.width)
      context.insert(taken)
      color.map(_.code).foreach { code =>
        val str = context.textMap(context.x, context.y)
        context.textMap(context.x, context.y) = code + str
        val end = context.textMap(context.x + taken.length - 1, context.y)
        context.textMap(context.x + taken.length - 1, context.y) = end + Console.RESET
      }
    }
  }

  case class Border(view: View) extends View {
    override def size(proposed: Size): Size = {
      view.size(proposed.scaled(-2, -2)).scaled(2, 2)
    }

    override def render(context: RenderContext, size: Size): Unit = {
      val childSize = view.size(size.scaled(-2, -2))

      val top    = "┌" + ("─" * childSize.width) + "┐"
      val bottom = "└" + ("─" * childSize.width) + "┘"

      // TODO: Clean up these rendering methods
      context.insert(top)
      context.textMap.insert(bottom, context.x, context.y + childSize.height + 1)
      (1 to childSize.height).foreach { dy =>
        context.textMap.add('│', context.x, context.y + dy)
        context.textMap.add('│', context.x + childSize.width + 1, context.y + dy)
      }
      context.textMap.insert(bottom, context.x, context.y + childSize.height + 1)
      context.scratch {
        context.translateBy(1, 1)
        view.render(context, childSize)
      }
    }
  }

  case class Overlay(view: View, overlay: View, alignment: Alignment) extends View { self =>
    override def size(proposed: Size): Size = view.size(proposed)

    override def render(context: RenderContext, size: Size): Unit = {
      view.render(context, size)
      context.scratch {
        val childSize = overlay.size(size)
        context.align(childSize, size, alignment)
        overlay.render(context, childSize)
      }
    }
  }

  case class FlexibleFrame(
      view: View,
      minWidth: Option[Int],
      maxWidth: Option[Int],
      minHeight: Option[Int],
      maxHeight: Option[Int],
      alignment: Alignment = Alignment.center
  ) extends View {
    override def size(proposed0: Size): Size = {
      var proposed = proposed0
      proposed = proposed.overriding(width = minWidth.filter(_ > proposed.width))
      proposed = proposed.overriding(width = maxWidth.filter(_ < proposed.width))
      proposed = proposed.overriding(height = minHeight.filter(_ > proposed.height))
      proposed = proposed.overriding(height = maxHeight.filter(_ < proposed.height))
      var result = view.size(proposed)
      minWidth.foreach { m =>
        result = result.copy(width = m.max(result.width.min(proposed.width)))
      }
      maxWidth.foreach { m =>
        result = result.copy(width = m.min(result.width.max(proposed.width)))
      }
      minHeight.foreach { m =>
        result = result.copy(height = m.max(result.height.min(proposed.height)))
      }
      maxHeight.foreach { m =>
        result = result.copy(height = m.min(result.height.max(proposed.height)))
      }
      result
    }

    override def render(context: RenderContext, size: Size): Unit = {
      context.scratch {
        val childSize = view.size(size)
        context.align(childSize, size, alignment)
        view.render(context, childSize)
      }
    }
  }

  case class FixedFrame(view: View, width: Option[Int], height: Option[Int], alignment: Alignment) extends View {
    override def size(proposed: Size): Size = {
      lazy val childSize = view.size(proposed.overriding(width, height))
      Size(width.getOrElse(childSize.width), height.getOrElse(childSize.height))
    }

    override def render(context: RenderContext, size: Size): Unit = {
      val childSize = view.size(size)
      context.scratch {
        context.align(childSize, size, alignment)
        view.render(context, childSize)
      }
    }
  }
}

object FrameExamples {

  def main(args: Array[String]): Unit = {
    val vertical = View
      .vertical(
        View
          .text("1. THAT IS JUST ABOUT HOW"),
        View
          .text("2. THAT IS JUST ABOUT HOW"),
        View
          .text("3. THAT IS JUST ABOUT HOW"),
        View
          .text("4. THAT IS JUST ABOUT HOW"),
        View
          .text("5. COCO")
      )

    val example = vertical
      .frame(11, 3, Alignment.bottomLeft)
      .bordered
      .overlay(
        View.text("BACKEND"),
        Alignment.bottom
      )

    val cool = View.Horizontal(Chunk(example, View.text("Funky"), example), 1, VerticalAlignment.Center)

    println(
      View
        .horizontal(
          View
            .text("zio-app")
            .center
            .bordered,
          View
            .text("zio-app")
            .frame(width = 3, height = 1)
            .bordered
        )
        .render(30, 7)
    )

    println(
      View
        .vertical(
          View
            .text("zio-app", Color.Red)
            .centerH
            .bordered,
          View
            .horizontal(
              View
                .vertical(
                  View.text("frontend starting..."),
                  View.text("frontend continue...")
                )
                .bottomLeft
                .bordered,
              View
                .vertical(
                  View.text("backend starting..."),
                  View.text("backend continue...")
                )
                .bottomLeft
                .bordered
                .overlay(View.text("BACKEND", Color.Cyan), Alignment.bottom)
            )
        )
        .render(103, 12)
    )
  }

}
