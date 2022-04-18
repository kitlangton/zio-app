package zio.app

import fansi.{Attr, Bold, Category, Str}
import zio.app.DevMode.{backendLines, frontendLines}
import zio.app.cli.protocol.{Attribute, Fragment, Line}
import zio.stream._
import zio._

trait SbtManager {
  def backendSbtStream: Stream[Throwable, Chunk[Line]]
  def frontendSbtStream: Stream[Throwable, Chunk[Line]]
  def launchVite: Stream[Throwable, Nothing]
}

object SbtManager {
  val live: ULayer[SbtManager] =
    ZLayer.succeed(SbtManagerLive())

  val backendSbtStream: ZStream[SbtManager, Throwable, Chunk[Line]] =
    ZStream.environmentWithStream[SbtManager](_.get.backendSbtStream)

  val frontendSbtStream: ZStream[SbtManager, Throwable, Chunk[Line]] =
    ZStream.environmentWithStream[SbtManager](_.get.frontendSbtStream)

  val launchVite: ZStream[SbtManager, Throwable, Nothing] =
    ZStream.environmentWithStream[SbtManager](_.get.launchVite)
}

case class SbtManagerLive() extends SbtManager {
  override def backendSbtStream: Stream[Throwable, Chunk[Line]] =
    backendLines
      .map { s =>
        val str = scala.util.Try(Str(s))
        str.map(renderDom).map(Chunk(_)).getOrElse(Chunk.empty)
      }
      .scan[Chunk[Line]](Chunk.empty)(_ ++ _)

  override def frontendSbtStream: Stream[Throwable, Chunk[Line]] =
    frontendLines
      .map { s =>
        val str = scala.util.Try(Str(s))
        str.map(renderDom).map(Chunk(_)).getOrElse(Chunk.empty)
      }
      .scan[Chunk[Line]](Chunk.empty)(_ ++ _)

  override def launchVite: Stream[Throwable, Nothing] =
    ZStream.fromZIO(DevMode.launchVite.exitCode).drain

  def renderDom(str: Str): Line = {
    val chars  = str.getChars
    val colors = str.getColors

    // Pre-size StringBuilder with approximate size (ansi colors tend
    // to be about 5 chars long) to avoid re-allocations during growth
    val output = new StringBuilder(chars.length + colors.length * 5)

    var section                 = new StringBuilder()
    var attrs: Chunk[Attribute] = Chunk.empty
    val builder                 = ChunkBuilder.make[Fragment]()

    var currentState: Str.State = 0

    // Make a local array copy of the immutable Vector, for maximum performance
    // since the Vector is small and we'll be looking it up over & over & over
    val categoryArray = Attr.categories.toArray

    var i = 0
    while (i < colors.length) {
      // Emit ANSI escapes to change colors where necessary
      // fast-path optimization to check for integer equality first before
      // going through the whole `enableDiff` rigmarole
      if (colors(i) != currentState) {
        emitAnsi(currentState, colors(i), output, categoryArray) match {
          case Some(newAttrs) if section.nonEmpty =>
            builder += Fragment(section.toString, attrs)
            attrs = newAttrs
            section = new StringBuilder()
          case _ =>
            ()
        }
        currentState = colors(i)
      }
      output.append(chars(i))
      section.append(chars(i))
      i += 1
    }

    builder += Fragment(section.toString, attrs)

    Line(builder.result())
  }

  def emitAnsi(
      currentState: Str.State,
      nextState: Str.State,
      output: StringBuilder,
      categoryArray: Array[Category]
  ): Option[Chunk[Attribute]] = {
    if (currentState != nextState) {
      val builder     = ChunkBuilder.make[Attribute]()
      val hardOffMask = Bold.mask

      val currentState2 =
        if ((currentState & ~nextState & hardOffMask) != 0) {
          output.append(scala.Console.RESET)
          0L
        } else {
          currentState
        }

      var categoryIndex = 0
      while (categoryIndex < categoryArray.length) {
        val cat = categoryArray(categoryIndex)
        if ((cat.mask & currentState2) != (cat.mask & nextState)) {
          val attr = cat.lookupAttr(nextState & cat.mask)
          attr.name match {
            case "Color.Red"     => builder += Attribute.Red
            case "Color.Yellow"  => builder += Attribute.Yellow
            case "Color.Blue"    => builder += Attribute.Blue
            case "Color.Green"   => builder += Attribute.Green
            case "Color.Magenta" => builder += Attribute.Magenta
            case "Color.Cyan"    => builder += Attribute.Cyan
            case "Bold.On"       => builder += Attribute.Bold
            case "Color.Reset"   => ()
            case _               => println(attr.name)
          }
          val escape = cat.lookupEscape(nextState & cat.mask)
          output.append(escape)
        }
        categoryIndex += 1
      }

      Some(builder.result())
    } else
      None
  }
}
