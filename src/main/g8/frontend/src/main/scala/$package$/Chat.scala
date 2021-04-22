package $package$

import _root_.boopickle.Default._
import animus._
import $package$.models.{ChatState, User}
import com.raquo.laminar.api.L._
import io.laminext.websocket._
import io.laminext.websocket.boopickle.WebSocketReceiveBuilderBooPickleOps
import $package$.models.User
import $package$.protocol.ClientCommand.Subscribe
import $package$.protocol.{ClientCommand, ServerCommand}

object Chat {
  val ws: WebSocket[ServerCommand, ClientCommand] =
    WebSocket
      .url(Config.webSocketsUrl)
      .pickle[ServerCommand, ClientCommand]
      .build(reconnectRetries = Int.MaxValue)

  val chatStateVar: Var[ChatState] = Var(ChatState.empty)
  val userVar: Var[Option[User]]   = Var(Option.empty[User])

  val \$connectionStatus: Signal[String] = ws.isConnected
    .combineWithFn(ws.isConnecting) {
      case (true, _) => "CONNECTED"
      case (_, true) => "CONNECTING"
      case _         => "OFFLINE"
    }

  def view: Div = {
    div(
      ws.connect,
      ws.connected --> { _ =>
        ws.sendOne(Subscribe)
      },
      ws.received --> { command =>
        println(s"RECEIVED COMMAND: \$command")
        command match {
          case ServerCommand.SendChatState(chatState) =>
            chatStateVar.set(chatState)
          case ServerCommand.SendUserId(id) =>
            userVar.set(Some(id))
        }
      },
      textAlign.center,
      div(
        position.relative,
        position.fixed,
        top("0"),
        bottom("0"),
        left("0"),
        right("0"),
        background("#111"),
        display.flex,
        alignItems.center,
        justifyContent.center,
        div(
          height("100%"),
          width("100%"),
          maxWidth("400px"),
          border("1px solid #333"),
          padding("24px"),
          maxHeight("600px"),
          minHeight("400px"),
          position.relative,
          InfoHeader,
          display.flex,
          flexDirection.column,
          div(
            height("100%"),
            children <-- chatStateVar.signal.map(_.messages.reverse.zipWithIndex).splitTransition(_._2) {
              case (_, (message, _), _, transition) =>
                val \$isSelf = userVar.signal.map { _.contains(message.user) }
                div(
                  padding("12px"),
                  marginBottom("8px"),
                  background <-- \$isSelf.map { if (_) "#223" else "#222" },
                  textAlign <-- \$isSelf.map { if (_) "right" else "left" },
                  fontSize("16px"),
                  div(
                    fontSize("12px"),
                    opacity(0.8),
                    message.user.userName,
                    fontStyle.italic,
                    marginBottom("8px")
                  ),
                  div(
                    message.content
                  ),
                  transition.height,
                  transition.opacity
                )
            },
            overflowY.hidden,
            inContext { el =>
              EventStream.periodic(1000 / 60) --> { _ =>
                el.ref.scrollTop = el.ref.scrollHeight
              }
            }
          ),
          textArea(
            autoFocus(true),
            height("120px"),
            alignSelf.flexEnd,
            inContext { el =>
              onKeyDown.filter(_.key == "Enter").preventDefault --> { _ =>
                val content = el.ref.value
                if (content.nonEmpty) {
                  ws.sendOne(ClientCommand.SendMessage(s"\$content"))
                  el.ref.value = ""
                }
              }
            }
          )
        )
      )
    )
  }

  def InfoHeader =
    div(
      display.flex,
      alignItems.flexStart,
      justifyContent.spaceBetween,
      paddingBottom("24px"),
      div(
        width("20px"),
        fontSize("16px"),
        div(
          "ZIGNAL"
        )
      ),
      div(
        fontSize("16px"),
        children <-- userVar.signal.map(_.map(_.userName).toList).splitTransition(identity) {
          (_, name, _, transition) =>
            em(
              transition.height,
              transition.opacity,
              color("yellow"),
              name
            )
        }
      ),
      div(
        width("20px"),
        background("#111"),
        zIndex(8),
        display.flex,
        flexDirection.column,
        alignItems.flexEnd,
        div(
          fontSize("14px"),
          opacity(0.7),
          div(
            display.flex,
            children <-- \$connectionStatus.splitOneTransition(identity) { (_, string, _, transition) =>
              div(string, transition.width, transition.opacity)
            },
            overflowY.hidden,
            height <-- EventStream
              .merge(
                \$connectionStatus.changes.debounce(5000).mapTo(false),
                \$connectionStatus.changes.mapTo(true)
              )
              .toSignal(false)
              .map {
                if (_) 20.0 else 0.0
              }
              .spring
              .px
          )
        ),
        div(
          opacity <-- Animation.from(0).wait(1000).to(1).run,
          display.flex,
          fontSize("14px"),
          div(s"POP.\${nbsp}", opacity(0.7)),
          AnimatedCount(chatStateVar.signal.map(_.connectedUsers.size))
        )
      )
    )
}
