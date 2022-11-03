package tui

import tui.TerminalApp.Step
import view.View.string2View
import view.{View, _}
import tui.components.{Choose, FancyComponent, LineInput}
import zio.stream._
import zio._

trait TerminalApp[-I, S, +A] { self =>
  def run(initialState: S): RIO[TUI, A] =
    runOption(initialState).map(_.get)

  def runOption(initialState: S): RIO[TUI, Option[A]] =
    TUI.run(self)(initialState)

  def render(state: S): View

  def update(state: S, event: TerminalEvent[I]): Step[S, A]
}

object TerminalApp {
  sealed trait Step[+S, +A]

  object Step {
    def update[S](state: S): Step[S, Nothing]   = Update(state)
    def succeed[A](result: A): Step[Nothing, A] = Done(result)
    def exit: Step[Nothing, Nothing]            = Exit

    private[tui] case class Update[S](state: S) extends Step[S, Nothing]
    private[tui] case class Done[A](result: A)  extends Step[Nothing, A]
    private[tui] case object Exit               extends Step[Nothing, Nothing]
  }
}

sealed trait TerminalEvent[+I]

trait TUI {
  def run[I, S, A](
    terminalApp: TerminalApp[I, S, A],
    events: ZStream[Any, Throwable, I],
    initialState: S
  ): Task[Option[A]]
}

object TUI {

  def live(fullScreen: Boolean): ZLayer[Any, Nothing, TUI] =
    ZLayer.succeed(TUILive(fullScreen))

  def run[I, S, A](terminalApp: TerminalApp[I, S, A])(initialState: S): RIO[TUI, Option[A]] =
    ZIO.serviceWithZIO[TUI](_.run(terminalApp, ZStream.never, initialState))

  def runWithEvents[I, S, A](
    terminalApp: TerminalApp[I, S, A]
  )(events: ZStream[Any, Throwable, I], initialState: S): RIO[TUI, Option[A]] =
    ZIO.serviceWithZIO[TUI](_.run(terminalApp, events, initialState))
}

case class TUILive(fullScreen: Boolean) extends TUI {
  var lastSize: Size = Size(0, 0)

  def run[I, S, A](
    terminalApp: TerminalApp[I, S, A],
    events: ZStream[Any, Throwable, I],
    initialState: S
  ): Task[Option[A]] =
    ZIO.scoped {
      Input
        .rawModeScoped(fullScreen)
        .flatMap { _ =>
          for {
            stateRef             <- SubscriptionRef.make(initialState)
            resultPromise        <- Promise.make[Nothing, Option[A]]
            oldMap: Ref[TextMap] <- Ref.make(TextMap.ofDim(0, 0))

            _ <- (for {
                   _               <- ZIO.succeed(Input.ec.clear())
                   (width, height) <- ZIO.succeed(Input.terminalSize)
                   _               <- renderFullScreen(oldMap, terminalApp, initialState, width, height)
                 } yield ()).when(fullScreen)

            renderStream =
              stateRef.changes
                .zipLatestWith(Input.terminalSizeStream)((_, _))
                .tap { case (state, (width, height)) =>
                  if (fullScreen) renderFullScreen(oldMap, terminalApp, state, width, height)
                  else renderTerminal(terminalApp, state)
                }

            updateStream = Input.keyEventStream.mergeEither(events).tap { keyEvent =>
                             val event = keyEvent match {
                               case Left(value)  => TerminalEvent.SystemEvent(value)
                               case Right(value) => TerminalEvent.UserEvent(value)
                             }

                             stateRef.updateZIO { state =>
                               terminalApp.update(state, event) match {
                                 case Step.Update(state) => ZIO.succeed(state)
                                 case Step.Done(result)  => resultPromise.succeed(Some(result)).as(state)
                                 case Step.Exit          => resultPromise.succeed(None).as(state)
                               }
                             }
                           }

            _      <- ZStream.mergeAllUnbounded()(renderStream, updateStream).interruptWhen(resultPromise.await).runDrain
            result <- resultPromise.await
          } yield result
        }
    }

  var lastHeight = 0
  var lastWidth  = 0

  def renderFullScreen[I, S, A](
    oldMap: Ref[TextMap],
    terminalApp: TerminalApp[I, S, A],
    state: S,
    width: Int,
    height: Int
  ): UIO[Unit] =
    oldMap.update { oldMap =>
      if (lastWidth != width || lastHeight != height) {
        lastHeight = height
        lastWidth = width
        val map = terminalApp.render(state).center.textMap(width, height)
        print(map.toString)
        map
      } else {
        val map  = terminalApp.render(state).center.textMap(width, height)
        val diff = TextMap.diff(oldMap, map, width, height)
        print(diff)
        map
      }
    }

  private def renderTerminal[I, S, A](terminalApp: TerminalApp[I, S, A], state: S): UIO[Unit] =
    ZIO.succeed {
      val (size, rendered) = terminalApp.render(state).renderNowWithSize

      Input.ec.moveUp(lastSize.height)
      Input.ec.clearToEnd()
      lastSize = size
      println(scala.Console.RESET + rendered + scala.Console.RESET)
    }
}

object TerminalAppExample extends ZIOAppDefault {
  override def run =
    (for {
      number <- Choose.run(List(1, 2, 3, 4, 5, 6))(_.toString.red.bold)
      line   <- LineInput.run("")
      _      <- FancyComponent.run(number.get + line.toIntOption.getOrElse(0))
    } yield ())
      .provide(TUI.live(false))
}

object TerminalEvent {
  case class UserEvent[+I](event: I)         extends TerminalEvent[I]
  case class SystemEvent(keyEvent: KeyEvent) extends TerminalEvent[Nothing]
}
