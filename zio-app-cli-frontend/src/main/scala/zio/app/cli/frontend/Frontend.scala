package zio.app.cli.frontend

import boopickle.Default._
import com.raquo.laminar.api.L._
import io.laminext.websocket.PickleSocket.WebSocketReceiveBuilderBooPickleOps
import io.laminext.websocket.WebSocket
import zio.app.cli.protocol._

object Frontend {
  val ws: WebSocket[ServerCommand, ClientCommand] =
    WebSocket
      .url("ws://localhost:8088/ws")
      .pickle[ServerCommand, ClientCommand]
      .build(reconnectRetries = Int.MaxValue)

  val outputVar = Var("")
  val inputVar  = Var("")

  val outputView = pre(
    child.text <-- outputVar
  )

  def view: Div =
    div(
      h1(s"Howdy"),
      ws.connect,
      outputView,
      input(
        controlled(
          value <-- inputVar,
          onInput.mapToValue --> inputVar
        )
      ),
      button(
        "SAY STUFF",
        onClick --> { _ =>
          ws.sendOne(ClientCommand.SayThis(inputVar.now()))
        }
      ),
      ws.connected --> { _ =>
        ws.sendOne(ClientCommand.Subscribe)
      },
      ws.received --> { (command: ServerCommand) =>
        command match {
          case ServerCommand.Message(string) =>
            println(s"MESSAGE $string")
          case ServerCommand.State(backendLines) =>
            outputVar.set(backendLines)
        }
      }
    )
}
