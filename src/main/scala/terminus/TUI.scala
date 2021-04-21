package terminus

import terminus.Input.KeyEvent
import terminus.TUI.TUILive
import zio.blocking.Blocking
import zio.clock.Clock
import zio.stream.ZStream
import zio.{Has, Promise, Queue, Ref, Tag, UIO, URIO, ZIO, ZLayer}

trait Term[State, A] {
  def exit: UIO[Unit]
  def succeed(a: A): UIO[Unit]
  def update(state: State => State): UIO[Unit]
}

case class TerminalComponent[State, A](
    update: KeyEvent => State => Term[State, A] => UIO[Unit],
    render: State => View
) { self =>
  def run(state: State)(implicit tag: Tag[State], tag2: Tag[A]): URIO[Has[TUI], A] = TUI.run(self, state)
}

trait TUI {
  def run[State: Tag, A: Tag](app: TerminalComponent[State, A], initialState: State): UIO[A]
}

object TUI {
  def run[State: Tag, A: Tag](app: TerminalComponent[State, A], initialState: State): URIO[Has[TUI], A] =
    ZIO.service[TUI].flatMap(_.run(app, initialState))

  val live: ZLayer[Blocking with Clock, Nothing, Has[TUI]] =
    ZIO.environment[Blocking with Clock].map(TUILive).toLayer

  case class TUILive(env: Blocking with Clock) extends TUI {
    var lastHeight: Int = 0

    override def run[State: Tag, A: Tag](app: TerminalComponent[State, A], initialState: State): UIO[A] =
      Input
        .withRawMode {
          for {
            stateRef       <- Ref.make(initialState)
            successPromise <- Promise.make[Nothing, A]
            exitPromise    <- Promise.make[Nothing, Unit]

            term = new Term[State, A] {
              override def exit: UIO[Unit]          = exitPromise.succeed(()).unit
              override def succeed(a: A): UIO[Unit] = successPromise.succeed(a).unit
              override def update(f: State => State): UIO[Unit] =
                stateRef.update(f)
            }

            loop <- ZStream
              .repeatEffect {
                for {
                  state <- stateRef.get
                  _     <- renderTerminal(app.render(state))
                  key   <- Input.keypress
                  _     <- app.update(key)(state)(term)
                } yield ()
              }
              .provide(env)
              .runDrain
              .fork

            a <- successPromise.await
            _ <- loop.interrupt
          } yield a
        }
        .provide(env)

    def renderTerminal(view: View): UIO[Unit] = UIO {
      val lines = view.render(frame = 10000).linesIterator.toList

      val newHeight = lines.length
      Input.ec.moveUp(lastHeight)
      Input.ec.clearToEnd()
      lastHeight = newHeight

      lines.foreach { line =>
        Input.ec.moveLeft(999)
        println(line)
      }
    }

  }
}
