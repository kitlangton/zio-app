package zio.app

import boopickle.Default._
import boopickle.{CompositePickler, UnpickleState}
import org.scalajs.dom.RequestMode
import sttp.client3._
import sttp.model.Uri
import zio._
import zio.app.internal.ZioResponse
import zio.stream._

import java.nio.{ByteBuffer, ByteOrder}
import scala.util.Try

object FrontendUtils {
  implicit val exPickler: CompositePickler[Throwable] = exceptionPickler

  private val sttpBackend =
    FetchZioBackend(fetchOptions = FetchOptions(credentials = None, mode = Some(RequestMode.`same-origin`)))

  def apiUri(config: ClientConfig): Uri =
    Uri(org.scalajs.dom.document.location.hostname)
      .scheme(org.scalajs.dom.document.location.protocol.replaceAll(":", ""))
      .port(org.scalajs.dom.document.location.port.toIntOption)
      .addPathSegments(config.root.add("api").segments)

  def fetch[E: Pickler, A: Pickler](service: String, method: String, config: ClientConfig): IO[E, A] =
    fetchRequest[E, A](bytesRequest.get(apiUri(config).addPath(service, method)), config)

  def fetch[E: Pickler, A: Pickler](
    service: String,
    method: String,
    value: ByteBuffer,
    config: ClientConfig
  ): IO[E, A] =
    fetchRequest[E, A](bytesRequest.post(apiUri(config).addPath(service, method)).body(value), config)

  def fetchRequest[E: Pickler, A: Pickler](request: Request[Array[Byte], Any], config: ClientConfig): IO[E, A] =
    sttpBackend
      .send(request.header("authorization", config.authToken.map("Bearer " + _)))
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
            // TODO: Fix constructor
            ZIO.interruptAs(FiberId(0, 0, Trace.empty))
        }
      }

  def fetchStream[E: Pickler, A: Pickler](service: String, method: String, config: ClientConfig): Stream[E, A] =
    fetchStreamRequest[E, A](basicRequest.get(apiUri(config).addPath(service, method)))

  def fetchStream[E: Pickler, A: Pickler](
    service: String,
    method: String,
    value: ByteBuffer,
    config: ClientConfig
  ): Stream[E, A] =
    fetchStreamRequest[E, A](basicRequest.post(apiUri(config).addPath(service, method)).body(value))

  def fetchStreamRequest[E: Pickler, A: Pickler](request: Request[Either[String, String], Any]): Stream[E, A] =
    ZStream.unwrap {
      request
        .response(asStreamAlwaysUnsafe(ZioStreams))
        .send(sttpBackend)
        .orDie
        .map(resp => transformZioResponseStream[E, A](resp.body))
    }

  private def transformZioResponseStream[E: Pickler, A: Pickler](stream: ZioStreams.BinaryStream) =
    stream
      .catchAll(ZStream.die(_))
      .mapConcatChunk(a => unpickleMany[E, A](a))
      .flatMap {
        case ZioResponse.Succeed(value) => ZStream.succeed(value)
        case ZioResponse.Fail(value)    => ZStream.fail(value)
        case ZioResponse.Die(throwable) => ZStream.die(throwable)
        case ZioResponse.Interrupt(_)   => ZStream.fromZIO(ZIO.interruptAs(FiberId(0, 0, Trace.empty)))
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
