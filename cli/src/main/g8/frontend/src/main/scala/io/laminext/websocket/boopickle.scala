package io.laminext.websocket
import _root_.boopickle.Default._
import org.scalajs.dom
import java.nio.ByteBuffer
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}

object boopickle {
  import scala.scalajs.js.typedarray.TypedArrayBufferOps._

  implicit class WebSocketReceiveBuilderBooPickleOps(b: WebSocketReceiveBuilder) {
    @inline def pickle[Receive, Send](implicit
        receiveDecoder: Pickler[Receive],
        sendEncoder: Pickler[Send]
    ): WebSocketBuilder[Receive, Send] =
      new WebSocketBuilder[Receive, Send](
        url = b.url,
        initializer = initialize.arraybuffer,
        sender = { (webSocket: dom.WebSocket, a: Send) =>
          val bytes: ByteBuffer = Pickle.intoBytes(a)
          val buffer            = bytes.arrayBuffer()
          send.arraybuffer.apply(webSocket, buffer)
        },
        receiver = { msg =>
          Right(Unpickle[Receive].fromBytes(TypedArrayBuffer.wrap(msg.data.asInstanceOf[ArrayBuffer])))
        }
      )
  }

}
