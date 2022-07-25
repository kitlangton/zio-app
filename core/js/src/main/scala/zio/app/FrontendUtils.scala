package zio.app

import boopickle.Default._
import boopickle.{CompositePickler, UnpickleState}
import org.scalajs.dom.RequestMode
import sttp.client3._
import zio._
import zio.stream._

import java.nio.{ByteBuffer, ByteOrder}
import scala.util.Try

object FrontendUtils {
  implicit val exPickler: CompositePickler[Throwable] = exceptionPickler

  private val sttpBackend =
    FetchZioBackend(fetchOptions = FetchOptions(credentials = None, mode = Some(RequestMode.`same-origin`)))

  def fetch[A: Pickler](service: String, method: String, config: ClientConfig): UIO[A] =
    fetchRequest[A](bytesRequest.get(uri"/api/$service/$method"), config)

  def fetch[A: Pickler](
    service: String,
    method: String,
    value: ByteBuffer,
    config: ClientConfig,
  ): UIO[A] =
    fetchRequest[A](bytesRequest.post(uri"/api/$service/$method").body(value), config)

  def fetchRequest[A: Pickler](request: Request[Array[Byte], Any], config: ClientConfig): UIO[A] =
    sttpBackend
      .send(request.header("authorization", config.authToken.map("Bearer " + _)))
      .orDie
      .flatMap { response =>
        Unpickle[A].fromBytes(ByteBuffer.wrap(response.body)) match {
          case value =>
            ZIO.succeed(value)
        }
      }

  def fetchStream[A: Pickler](service: String, method: String): Stream[Nothing, A] =
    fetchStreamRequest[A](basicRequest.get(uri"/api/$service/$method"))

  def fetchStream[A: Pickler](service: String, method: String, value: ByteBuffer): Stream[Nothing, A] =
    fetchStreamRequest[A](basicRequest.post(uri"/api/$service/$method").body(value))

  def fetchStreamRequest[A: Pickler](request: Request[Either[String, String], Any]): Stream[Nothing, A] =
    ZStream.unwrap {
      request
        .response(asStreamAlwaysUnsafe(ZioStreams))
        .send(sttpBackend)
        .orDie
        .map(resp => transformZioResponseStream[A](resp.body))
    }

  private def transformZioResponseStream[A: Pickler](stream: ZioStreams.BinaryStream) =
    stream
      .catchAll(ZStream.die(_))
      .mapConcatChunk(a => unpickleMany[A](a))

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
        redirectToGet = true,
      ),
      Map(),
    )

  def unpickleMany[A: Pickler](bytes: Array[Byte]): Chunk[A] = {
    val unpickleState       = UnpickleState(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN))
    def unpickle: Option[A] = Try(Unpickle[A].fromState(unpickleState)).toOption
    Chunk.unfold(unpickle)(_.map(_ -> unpickle))
  }

}
