package zio.app

import boopickle.Default._
import boopickle.{CompositePickler, UnpickleState}
import org.scalajs.dom.experimental.RequestMode
import sttp.client3._
import zio._
import zio.app.internal.ZioResponse
import zio.stream._

import java.nio.{ByteBuffer, ByteOrder}
import scala.util.Try

object FrontendUtils {
  implicit val exPickler: CompositePickler[Throwable] = exceptionPickler

  private val sttpBackend =
    FetchZioBackend(fetchOptions = FetchOptions(credentials = None, mode = Some(RequestMode.cors)))

  def fetch[E: Pickler, A: Pickler](uriPrefix: String, service: String, method: String): IO[E, A] =
    fetchRequest[E, A](bytesRequest.get(uri"$uriPrefix/$service/$method"))

  def fetch[E: Pickler, A: Pickler](uriPrefix: String, service: String, method: String, value: ByteBuffer): IO[E, A] =
    fetchRequest[E, A](bytesRequest.post(uri"$uriPrefix/$service/$method").body(value))

  def fetchRequest[E: Pickler, A: Pickler](request: Request[Array[Byte], Any]): IO[E, A] =
    sttpBackend
      .send(request)
      .orDie
      .flatMap { response =>
        Unpickle[ZioResponse[E, A]].fromBytes(ByteBuffer.wrap(response.body)) match {
          case ZioResponse.Succeed(value) =>
            ZIO.succeed(value)
          case ZioResponse.Fail(value) =>
            ZIO.fail(value)
          case ZioResponse.Die(throwable) =>
            ZIO.die(throwable)
          case ZioResponse.Interrupt(fiberId) =>
            ZIO.interruptAs(Fiber.Id(0, fiberId))
        }
      }

  def fetchStream[E: Pickler, A: Pickler](uriPrefix: String, service: String, method: String): Stream[E, A] = {
    ZStream
      .unwrap {
        basicRequest
          .get(uri"$uriPrefix/$service/$method")
          .response(asStreamAlwaysUnsafe(ZioStreams))
          .send(sttpBackend)
          .orDie
          .map(resp => transformZioResponseStream[E, A](resp.body))
      }
  }

  def fetchStream[E: Pickler, A: Pickler](
      uriPrefix: String,
      service: String,
      method: String,
      value: ByteBuffer
  ): Stream[E, A] = ZStream
      .unwrap {
        basicRequest
          .post(uri"$uriPrefix/$service/$method")
          .body(value)
          .response(asStreamAlwaysUnsafe(ZioStreams))
          .send(sttpBackend)
          .orDie
          .map(resp => transformZioResponseStream[E, A](resp.body))
      }

  private def transformZioResponseStream[E: Pickler, A: Pickler](stream: ZioStreams.BinaryStream) = {
    stream
      .catchAll(ZStream.die(_))
      .mapConcatChunk(a => unpickleMany[E, A](a))
      .flatMap {
        case ZioResponse.Succeed(value)     => ZStream.succeed(value)
        case ZioResponse.Fail(value)        => ZStream.fail(value)
        case ZioResponse.Die(throwable)     => ZStream.die(throwable)
        case ZioResponse.Interrupt(fiberId) => ZStream.fromEffect(ZIO.interruptAs(Fiber.Id(0, fiberId)))
      }
  }

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

  def unpickleMany[E: Pickler, A: Pickler](bytes: Array[Byte]): Chunk[ZioResponse[E, A]] = {
    val unpickleState                       = UnpickleState(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN))
    def unpickle: Option[ZioResponse[E, A]] = Try(Unpickle[ZioResponse[E, A]].fromState(unpickleState)).toOption
    Chunk.unfold(unpickle)(_.map(_ -> unpickle))
  }

}
