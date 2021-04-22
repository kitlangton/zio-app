package $package$

import org.scalajs.dom.window

object Config {
  val isLocalHost: Boolean = window.location.host.startsWith("localhost")

  val webSocketsUrl: String =
    if (isLocalHost) "ws://localhost:8088/ws"
    else "wss://young-brushlands-01236.herokuapp.com/ws"
}
