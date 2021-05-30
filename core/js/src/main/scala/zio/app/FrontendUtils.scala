package zio.app

import boopickle.Default._
import boopickle.UnpickleState
import org.scalajs.dom.experimental.RequestMode
import sttp.client3._
import zio._
import zio.app.internal.ZioResponse
import zio.stream._

import java.nio.{ByteBuffer, ByteOrder}
import scala.util.Try

object FrontendUtils {

  private val sttpBackend =
    FetchZioBackend(fetchOptions = FetchOptions(credentials = None, mode = Some(RequestMode.`same-origin`)))

  def fetch[A: Pickler](service: String, method: String): UIO[A] =
    fetchRequest[A](bytesRequest.get(uri"api/$service/$method"))

  def fetch[A: Pickler](service: String, method: String, value: ByteBuffer): UIO[A] =
    fetchRequest[A](bytesRequest.post(uri"api/$service/$method").body(value))

  def fetchRequest[A: Pickler](request: Request[Array[Byte], Any]): UIO[A] =
    sttpBackend
      .send(request)
      .map { response =>
        Unpickle[A].fromBytes(ByteBuffer.wrap(response.body))
      }
      .orDie

  implicit val exPickler = exceptionPickler

  def fetchStream[E: Pickler, A: Pickler](service: String, method: String): Stream[E, A] =
    ZStream
      .unwrap {
        basicRequest
          .get(uri"api/$service/$method")
          .response(asStreamAlwaysUnsafe(ZioStreams))
          .send(sttpBackend)
          .catchAll(ZIO.die(_))
          .map(
            _.body
              .catchAll(ZStream.die(_))
              .mapConcatChunk(a => unpickleMany[E, A](a))
              .flatMap {
                case ZioResponse.Succeed(value)     => ZStream.succeed(value)
                case ZioResponse.Fail(value)        => ZStream.fail(value)
                case ZioResponse.Die(throwable)     => ZStream.die(throwable)
                case ZioResponse.Interrupt(fiberId) => ZStream.fromEffect(ZIO.interruptAs(Fiber.Id(0, fiberId)))
              }
          )
      }

  def fetchStream[E: Pickler, A: Pickler](service: String, method: String, value: ByteBuffer): Stream[E, A] =
    ZStream
      .unwrap {
        basicRequest
          .post(uri"api/$service/$method")
          .body(value)
          .response(asStreamAlwaysUnsafe(ZioStreams))
          .send(sttpBackend)
          .catchAll(ZIO.die(_))
          .map(
            _.body
              .catchAll(ZStream.die(_))
              .mapConcatChunk(a => unpickleMany[E, A](a))
              .flatMap {
                case ZioResponse.Succeed(value)     => ZStream.succeed(value)
                case ZioResponse.Fail(value)        => ZStream.fail(value)
                case ZioResponse.Die(throwable)     => ZStream.die(throwable)
                case ZioResponse.Interrupt(fiberId) => ZStream.fromEffect(ZIO.interruptAs(Fiber.Id(0, fiberId)))
              }
          )
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
