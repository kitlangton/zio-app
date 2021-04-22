package $package$

import com.raquo.laminar.api.L._
import animus._

case class AnimatedCount(\$count: Signal[Int]) extends Component {
  val \$digits: Signal[List[(String, Int)]] = \$count.map(_.toString.split("").reverse.zipWithIndex.reverse.toList)

  override def body: HtmlElement =
    div(
      textAlign.center,
      fontFamily("Source Code Pro"),
      display.flex,
      justifyContent.center,
      children <-- \$digits.splitTransition(_._2) { case (_, _, signal, t0) =>
        div(
          position.relative,
          children <-- signal
            .map(_._1)
            .splitOneTransition(identity) { (_, int, _, t1) =>
              div(
                int,
                transform <-- t1.\$isActive.map { if (_) 1.0 else 0.0 }.spring.map { s => s"scaleY(\$s)" },
                transformOrigin("top"),
                t1.opacity,
                t1.height,
                t0.width
              )
            }
            .map(_.reverse)
        )
      }
    )
}
