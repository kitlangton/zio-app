import com.raquo.laminar.api.L._
import animus._

package object components {
  def FadeInWords(string: String, \$active0: Signal[Boolean]): Modifier[HtmlElement] = {
    string.split(" ").zipWithIndex.toList.map { case (word, idx) =>
      val \$active =
        \$active0.flatMap {
          case true =>
            EventStream.fromValue(true).delay(idx * 100).startWith(false)
          case false => Val(false)
        }

      div(
        word + nbsp,
        lineHeight("1.5"),
        display.inlineFlex,
        Transitions.opacity(\$active),
        position.relative,
        Transitions.height(\$active),
        onMountBind { el =>
          top <-- \$active.map { if (_) 0.0 else el.thisNode.ref.scrollHeight.toDouble }.spring.px
        }
      )
    }
  }

}
