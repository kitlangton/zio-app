package terminus

import org.jline.terminal.Terminal.{Signal, SignalHandler}
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp.Capability
import zio._
import zio.blocking.{Blocking, effectBlocking, effectBlockingInterrupt}
import zio.clock.Clock
import zio.stream.{UStream, ZStream}

object Input {

  private val terminal: org.jline.terminal.Terminal =
    TerminalBuilder
      .builder()
      .jna(true)
      .system(true)
      .build();

  // [improve]
  lazy val terminalSizeStream: UStream[(Int, Int)] =
    ZStream
      .unwrap {
        for {
          queue <- Queue.bounded[(Int, Int)](32)
          _     <- queue.offer(size)
          _ <-
            ZStream
              .effectAsync[Any, Option[Nothing], Boolean] { register =>
                addResizeHandler(size => register(queue.offer(size).map(Chunk(_))))
              }
              .runDrain
              .fork
          stream = ZStream.fromQueue(queue)
        } yield stream
      }

  private def addResizeHandler(f: ((Int, Int)) => Unit): SignalHandler =
    terminal.handle(Signal.WINCH, _ => { f(size) })

  def enableRawMode: URIO[Blocking, Unit] =
    blocking
      .effectBlockingInterrupt {
        terminal.enterRawMode()
        ec.hideCursor()
      }
      .unit
      .orDie

  lazy val ec = new EscapeCodes(System.out)

  def size: (Int, Int) = {
    val size   = Input.terminal.getSize
    val width  = size.getColumns
    val height = size.getRows
    (width, height)
  }

  def disableRawMode: URIO[Blocking, Unit] =
    effectBlocking {
      terminal.puts(Capability.exit_ca_mode)
      ec.showCursor()
    }.unit.orDie

  sealed trait Event
  sealed trait KeyEvent extends Event
  object KeyEvent {
    case class UnSupported(bytes: Chunk[Int]) extends KeyEvent
    case class Character(char: Char)          extends KeyEvent
    case object Up                            extends KeyEvent
    case object Down                          extends KeyEvent
    case object Left                          extends KeyEvent
    case object Right                         extends KeyEvent
    case object Enter                         extends KeyEvent
    case object Delete                        extends KeyEvent
    case object Escape                        extends KeyEvent
    case object Tab                           extends KeyEvent
    case object ShiftTab                      extends KeyEvent
    case object Exit                          extends KeyEvent
  }

  def chunkToKeyEvent(chunk: Chunk[Int]): KeyEvent =
    chunk.toList match {
      case List(27, 91, 65) => KeyEvent.Up
      case List(27, 91, 66) => KeyEvent.Down
      case List(27, 91, 67) => KeyEvent.Right
      case List(27, 91, 68) => KeyEvent.Left
      case List(13)         => KeyEvent.Enter
      case List(3)          => KeyEvent.Exit
      case List(127)        => KeyEvent.Delete
      case List(27)         => KeyEvent.Escape
      case ::(head, Nil)    => KeyEvent.Character(head.toChar)
      case _                => KeyEvent.UnSupported(chunk)
    }

  def withRawMode[R, E, A](zio: ZIO[R, E, A]): ZIO[R with Blocking, E, A] =
    ZIO.bracket(enableRawMode)(_ => disableRawMode)(_ => zio)

  // TODO: Make this less horrible
  def keypress: ZIO[Blocking with Clock, Nothing, KeyEvent] = effectBlockingInterrupt {
    val n = System.in.read()
    n match {
      case 27 =>
        Thread.sleep(20)
        if (System.in.available() != 0) {
          val k = System.in.read()
          if (k == 91) {
            val o = System.in.read()
            o match {
              case 48 => // Status OK from status, used as a resize signal
                System.in.read()
                KeyEvent.Character(48.toChar)
              case 65 => KeyEvent.Up
              case 66 => KeyEvent.Down
              case 67 => KeyEvent.Right
              case 68 => KeyEvent.Left
              case 90 => KeyEvent.ShiftTab
              case _  => KeyEvent.Character((10000 + o).toChar)
            }
          } else KeyEvent.Character((20000 + k).toChar)
        } else KeyEvent.Escape
      case 13  => KeyEvent.Enter
      case 3   => KeyEvent.Exit
      case 127 => KeyEvent.Delete
      case _   => KeyEvent.Character(n.toChar)
    }
  }.orDie
}
