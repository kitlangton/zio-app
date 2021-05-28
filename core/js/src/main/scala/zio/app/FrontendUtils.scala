package zio.app

import boopickle.Default._
import boopickle.{BufferPool, DecoderSize, UnpickleState}
import org.scalajs.dom.experimental.RequestMode
import sttp.client3._
import zio._
import zio.stream._

import java.nio.{ByteBuffer, ByteOrder}
import scala.util.Try

object FrontendUtils {

  private val sttpBackend =
    FetchZioBackend(fetchOptions = FetchOptions(credentials = None, mode = Some(RequestMode.`same-origin`)))

  def fetch[A: Pickler](service: String, method: String): UIO[A] =
    fetchRequest(bytesRequest.get(uri"api/$service/$method"))

  def fetch[A: Pickler](service: String, method: String, value: ByteBuffer): UIO[A] =
    fetchRequest[A](bytesRequest.post(uri"api/$service/$method").body(value))

  def fetchRequest[A: Pickler](request: Request[Array[Byte], Any]): UIO[A] =
    sttpBackend
      .send(request)
      .map { response =>
        Unpickle[A].fromBytes(ByteBuffer.wrap(response.body))
      }
      .orDie

  def fetchStream[A: Pickler](service: String, method: String): Stream[Nothing, A] =
    ZStream
      .unwrap {
        basicRequest
          .get(uri"api/$service/$method")
          .response(asStreamAlwaysUnsafe(ZioStreams))
          .send(sttpBackend)
          .map(
            _.body
              .map { b =>
                println("BYTES")
                b
              }
              .mapConcatChunk[A](a => unpickleMany[A](a))
          )
      }
      .catchAll(ZStream.die(_))

  def fetchStream[A: Pickler](service: String, method: String, value: ByteBuffer): Stream[Nothing, A] =
    ZStream
      .unwrap {
        basicRequest
          .post(uri"api/$service/$method")
          .body(value)
          .response(asStreamAlwaysUnsafe(ZioStreams))
          .send(sttpBackend)
          .map(_.body.mapConcatChunk[A](a => unpickleMany[A](a)))
      }
      .catchAll(ZStream.die(_))

  private val bytesRequest =
    RequestT[Empty, Array[Byte], Any](
      None,
      None,
      NoBody,
      Vector(),
      asByteArrayAlways,
      RequestOptions(
        followRedirects = true,
        DefaultReadTimeout,
        10,
        redirectToGet = true
      ),
      Map()
    )

  BufferPool.disable()

  def unpickleMany[A: Pickler](bytes: Array[Byte]): Chunk[A] = {
    val unpickleState       = UnpickleState(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN))
    def unpickle: Option[A] = Try(Unpickle[A].fromState(unpickleState)).toOption
    Chunk.unfold(unpickle)(_.map(_ -> unpickle))
  }

}
