package zio.app.cli.frontend

import animus._
import boopickle.Default._
import com.raquo.laminar.api.L._
import io.laminext.websocket.PickleSocket.WebSocketReceiveBuilderBooPickleOps
import io.laminext.websocket.WebSocket
import org.scalajs.dom.window
import zio.Chunk
import zio.app.cli.protocol._

sealed trait ConnectionStatus

object ConnectionStatus {
  case object Online     extends ConnectionStatus
  case object Connecting extends ConnectionStatus
  case object Offline    extends ConnectionStatus
}

object Frontend {
  val ws: WebSocket[ServerCommand, ClientCommand] =
    WebSocket
      .url("ws://localhost:9630/ws")
      .pickle[ServerCommand, ClientCommand]
      .build(reconnectRetries = Int.MaxValue)

  val $connectionStatus: Signal[ConnectionStatus] =
    ws.isConnecting.combineWith(ws.isConnected).map {
      case (_, true)  => ConnectionStatus.Online
      case (true, _)  => ConnectionStatus.Connecting
      case (false, _) => ConnectionStatus.Offline
    }

  val connectionIndicator = div(
    cls("status-indicator"),
    width("8px"),
    height("8px"),
    borderRadius("8px"),
    backgroundColor <-- $connectionStatus.map {
      case ConnectionStatus.Online     => "green"
      case ConnectionStatus.Connecting => "yellow"
      case ConnectionStatus.Offline    => "red"
    }
  )

  val stateVar = Var(ServerCommand.State(Chunk.empty, Chunk.empty, FileSystemState("", List.empty)))
  val inputVar = Var("")

  sealed trait Focus

  object Focus {
    case object Frontend extends Focus
    case object Backend  extends Focus
    case object None     extends Focus
  }

  case class AppState(focus: Focus) {
    def focusFrontend: AppState =
      copy(focus = focus match {
        case Focus.Frontend => Focus.None
        case _              => Focus.Frontend
      })

    def focusBackend: AppState =
      copy(focus = focus match {
        case Focus.Backend => Focus.None
        case _             => Focus.Backend
      })
  }

  val appStateVar = Var(AppState(Focus.None))

  case class Rect(x: Double, y: Double, width: Double, height: Double) {}

  object Rect {
    def styles(rect: Signal[Rect]): Mod[HtmlElement] = Seq(
      left <-- rect.map(_.x).spring.px,
      top <-- rect.map(_.y).spring.px,
      width <-- rect.map(_.width).spring.px,
      height <-- rect.map(_.height).spring.px
    )
  }

  case class Grid(backend: HtmlElement, frontend: HtmlElement) extends Component {
    val rectVar = Var(Rect(0.0, 0.0, window.innerWidth, window.innerHeight))

    val $width = rectVar.signal.map(_.width / 2).spring.px

    case class Layout(backendRect: Rect, frontendRect: Rect)

    def layout(available: Rect, focus: Focus): Layout = {
      val width = available.width / 2
      if (width < 500) {
        val backendHeight = focus match {
          case Focus.Frontend => 30.0
          case Focus.Backend  => available.height - 30.0
          case Focus.None     => available.height / 2.0
        }

        val backend  = Rect(0, 0, available.width, backendHeight)
        val frontend = Rect(0, backendHeight, available.width, available.height - backendHeight)
        Layout(backend, frontend)
      } else {
        // Horizontal Layout
        val backendWidth = focus match {
          case Focus.Frontend => 30.0
          case Focus.Backend  => available.width - 30.0
          case Focus.None     => available.width / 2.0
        }

        val backend  = Rect(0, 0, backendWidth, available.height)
        val frontend = Rect(backendWidth, 0, available.width - backendWidth, available.height)
        Layout(backend, frontend)
      }
    }

    val $layout = rectVar.signal.combineWithFn(appStateVar.signal.map(_.focus))(layout)

    def body: HtmlElement =
      div(
        cls("main-grid"),
        overflow.hidden,
        position.relative,
        onMountBind { el =>
          EventStream.periodic(100) --> { _ =>
            val rect  = el.thisNode.ref.getBoundingClientRect()
            val rect1 = Rect(rect.left, rect.top, rect.width, rect.height)
            rectVar.set(rect1)
          }
        },
        div(
          backend,
          position.absolute,
          Rect.styles($layout.map(_.backendRect))
        ),
        div(
          frontend,
          position.absolute,
          Rect.styles($layout.map(_.frontendRect))
        )
      )
  }

  private def sbtOutputs =
    Grid(
      SbtOutput(
        stateVar.signal.map(_.backendLines),
        appStateVar.signal.map(_.focus != Focus.Frontend),
        () => appStateVar.update(_.focusBackend),
        "BACKEND"
      ),
      SbtOutput(
        stateVar.signal.map(_.frontendLines),
        appStateVar.signal.map(_.focus != Focus.Backend),
        () => appStateVar.update(_.focusFrontend),
        "FRONTEND"
      )
    )

  def header: Div = div(
    cls("top-header"),
    s"zio-app",
    div(
      display.flex,
      alignItems.center,
      a(
        fontSize("12px"),
        "http://localhost:3000",
        href("http://localhost:3000"),
        target("_blank"),
        textDecoration.none,
        color("white"),
        padding("4px 6px"),
        borderRadius("2px"),
        background("rgb(25,25,25")
      ),
      div(width("12px")),
      connectionIndicator
    )
  )

  val $fileSystem = stateVar.signal.map(_.fileSystemState)

  def fileSystem: Div = {
    div(
      fontSize("16px"),
      child.text <-- $fileSystem.map(_.pwd),
      children <-- $fileSystem.map(_.dirs).splitTransition(identity) { (_, path, _, transition) =>
        div(
          onClick --> { _ =>
            ws.sendOne(ClientCommand.ChangeDirectory(path))
          },
          div(
            cls("fs-item"),
            cursor.pointer,
            path
          ),
          transition.height,
          transition.opacity
        )
      }
    )
  }

  def view: Div =
    div(
      cls("container"),
      header,
      sbtOutputs,
      windowEvents.onKeyDown --> {
        _.key match {
          case "f" => appStateVar.update(_.focusFrontend)
          case "b" => appStateVar.update(_.focusBackend)
          case _   => ()
        }
      },
      ws.connect,
      ws.connected --> { _ =>
        ws.sendOne(ClientCommand.Subscribe)
      },
      ws.received --> { (command: ServerCommand) =>
        command match {
          case state: ServerCommand.State =>
            stateVar.set(state)
        }
      }
    )

}
