package zio.app.cli.frontend

import animus._
import com.raquo.laminar.api.L._
import zio.Chunk
import zio.app.cli.protocol
import zio.app.cli.protocol.Line

case class SbtOutput(
    $lines: Signal[Chunk[Line]],
    $isOpen: Signal[Boolean],
    toggleVisible: () => Unit,
    title: String
) extends Component {

  val scrollTopVar   = Var(0.0)
  val scrollOverride = Var(false)

  def collapse = {
    val $deg = $isOpen.map { b =>
      if (!b) 90.0
      else 180.0
    }

    div(
      cls("hover-button"),
      cursor.pointer,
      div(
        "^",
        transform <-- $deg.spring.map { deg => s"rotate(${deg}deg)" }
      ),
      onClick --> { _ => toggleVisible() }
    )
  }

  def scrollLocking = {
    val $deg = scrollOverride.signal.map { b =>
      if (!b) 90.0
      else 180.0
    }

    div(
      cls("hover-button"),
      cursor.pointer,
      div(
        "^",
        transform <-- $deg.spring.map { deg => s"rotate(${deg}deg)" }
      ),
      onClick --> { _ => scrollOverride.update(!_) }
    )
  }

  val $headerPadding = $isOpen.map { if (_) 12.0 else 0.0 }.spring.map { p => s"${p}px 12px" }

  def body: HtmlElement =
    div(
      cls("sbt-output"),
      cls.toggle("disabled") <-- $isOpen.map(!_),
      div(
        cls("sbt-output-title"),
        zIndex(10),
        div(
          title,
          opacity <-- $isOpen.map { if (_) 1.0 else 0.5 }.spring
        ),
        div(
          display.flex,
          collapse,
          div(width("12px")),
          scrollLocking
        )
      ),
      pre(
        cls("sbt-output-body"),
        visibilityStyles,
        div(
          children <-- $rendered,
          opacity <-- $opacity.spring
        ),
        scrollEvents
      )
    )

  lazy val scrollEvents: Modifier[HtmlElement] = Seq(
    inContext { (el: HtmlElement) =>
      val diffBus = new EventBus[Double]
      val ref     = el.ref
      Seq(
        $lines.changes.delay(0).combineWith(EventStream.periodic(100)) --> { _ =>
          if (!scrollOverride.now()) {
            scrollTopVar.set(
              ref.scrollHeight.toDouble - ref.getBoundingClientRect().height
            )
          }
        },
        onWheel --> { _ =>
          val diff: Double = ref.scrollHeight - (ref.scrollTop + ref.getBoundingClientRect().height)
          diffBus.writer.onNext(diff)
          scrollOverride.set(true)
          scrollTopVar.set(ref.scrollTop)
        },
        diffBus.events.debounce(100) --> { diff =>
          scrollOverride.set(diff > 0)
        },
        // TODO: Fix `step` method in animus. Make sure to check that animating is still true.
        scrollTopVar.signal.spring --> { scrollTop =>
          if (!scrollOverride.now()) {
            ref.scrollTop = scrollTop
          }
        }
      )
    }
  )

  def attrStyles(attribute: protocol.Attribute): Mod[HtmlElement] = attribute match {
    case protocol.Attribute.Red     => cls("console-red")
    case protocol.Attribute.Yellow  => cls("console-yellow")
    case protocol.Attribute.Blue    => cls("console-blue")
    case protocol.Attribute.Green   => cls("console-green")
    case protocol.Attribute.Cyan    => cls("console-cyan")
    case protocol.Attribute.Magenta => cls("console-magenta")
    case protocol.Attribute.Bold    => fontWeight.bold
  }

  val $rendered = $lines.map(_.zipWithIndex.toVector).split(_._2) { (_, value, _) =>
    div(
      value._1.fragments.map { fragment =>
        span(fragment.attributes.map(attrStyles), fragment.string)
      }
    )
  }

  private val $height  = $isOpen.map { if (_) 200.0 else 0.0 }
  private val $padding = $isOpen.map { if (_) 12.0 else 0.0 }
  private val $opacity = $isOpen.map { if (_) 1.0 else 0.3 }

  private val visibilityStyles: Mod[HtmlElement] = Seq(
//    height <-- $height.spring.px,
    paddingTop <-- $padding.spring.px,
    paddingBottom <-- $padding.spring.px
  )
}
