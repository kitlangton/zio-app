package zio.app

import boopickle.Default.{Pickler, Unpickle}
import org.scalajs.dom.experimental.RequestMode
import sttp.capabilities
import sttp.client3._
import zio._

import java.nio.ByteBuffer
import scala.concurrent.Future

object FrontendUtils {
  private val sttpBackend: SttpBackend[Future, capabilities.WebSockets] =
    FetchBackend(FetchOptions(mode = Some(RequestMode.`same-origin`), credentials = None))

  def fetch[A: Pickler](service: String, method: String): UIO[A] =
    fetchRequest(bytesRequest.get(uri"api/$service/$method"))

  def fetch[A: Pickler](service: String, method: String, value: ByteBuffer): UIO[A] =
    fetchRequest[A](
      bytesRequest.post(uri"api/$service/$method").body(value)
    )

  def fetchRequest[A: Pickler](request: Request[Array[Byte], Any]): UIO[A] =
    ZIO
      .fromFuture(implicit ec => {
        sttpBackend
          .send(request)
          .map { response =>
            val bytes = ByteBuffer.wrap(response.body)
            Unpickle[A].fromBytes(bytes)
          }
      })
      .orDie

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

}
