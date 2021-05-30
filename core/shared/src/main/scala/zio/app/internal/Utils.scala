package zio.app.internal

import boopickle.Default._
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.{HttpHeaderNames, HttpHeaderValues}
import zhttp.http._
import zio._
import zio.stream.ZStream

import java.nio.ByteBuffer

object Utils {
  private val bytesContent: Header = Header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.BYTES)

  private def pickle[A: Pickler](value: A): UResponse = {
    val bytes: ByteBuffer = Pickle.intoBytes(value)
    val byteBuf           = Unpooled.wrappedBuffer(bytes)
    val httpData          = HttpData.fromByteBuf(byteBuf)

    Response.http(status = Status.OK, headers = List(bytesContent), content = httpData)
  }

  def makeRoute[R, E, A: Pickler, B: Pickler](
      service: String,
      method: String,
      call: A => ZIO[R, E, B]
  ): HttpApp[R, E] = {
    val service0 = service
    val method0  = method
    Http.collectM { case post @ Method.POST -> Root / `service0` / `method0` =>
      post.content match {
        case HttpData.CompleteData(data) =>
          val byteBuffer = ByteBuffer.wrap(data.toArray)
          val unpickled  = Unpickle[A].fromBytes(byteBuffer)
          call(unpickled).map(pickle[B](_))
        case _ => UIO(Response.ok)
      }
    }
  }

  def makeRouteNullary[R, E, A: Pickler](
      service: String,
      method: String,
      call: ZIO[R, E, A]
  ): HttpApp[R, E] = {
    val service0 = service
    val method0  = method
    Http.collectM { case Method.GET -> Root / `service0` / `method0` =>
      call.map(pickle[A](_))
    }
  }

  def makeRouteStream[R, E: Pickler, A: Pickler, B: Pickler](
      service: String,
      method: String,
      call: A => ZStream[R, E, B]
  ): HttpApp[R, E] = {
    val service0 = service
    val method0  = method
    Http.collect { case post @ Method.POST -> Root / `service0` / `method0` =>
      post.content match {
        case HttpData.CompleteData(data) =>
          val byteBuffer = ByteBuffer.wrap(data.toArray)
          val unpickled  = Unpickle[A].fromBytes(byteBuffer)
          makeStreamResponse(call(unpickled))
        case _ => Response.ok
      }
    }
  }

  def makeRouteNullaryStream[R, E: Pickler, A: Pickler](
      service: String,
      method: String,
      call: ZStream[R, E, A]
  ): HttpApp[R, Nothing] = {
    val service0 = service
    val method0  = method
    Http.collect { case Method.GET -> Root / `service0` / `method0` =>
      makeStreamResponse(call)
    }
  }

  implicit val exPickler = exceptionPickler

  private def makeStreamResponse[A: Pickler, E: Pickler, R](
      stream: ZStream[R, E, A]
  ): Response.HttpResponse[R, Nothing] = {
    val responseStream: ZStream[R, Nothing, Byte] =
      stream
        .map(ZioResponse.succeed)
        .catchAllCause { cause =>
          cause.find {
            case Cause.Fail(failure) =>
              println("SENDING FAILURE")
              ZStream(ZioResponse.fail(failure))
            case Cause.Die(die)           => ZStream(ZioResponse.die(die))
            case Cause.Interrupt(fiberId) => ZStream(ZioResponse.interrupt(fiberId.seqNumber))
          }.get
        }
        .mapConcatChunk { a =>
          println(s"WOW CHUNK ${a}")
          Chunk.fromByteBuffer(Pickle.intoBytes(a))
        }

    Response.http(content = HttpData.fromStream(responseStream))
  }
}

object CustomPicklers {
  implicit val nothingPickler: Pickler[Nothing] = new Pickler[Nothing] {
    override def pickle(obj: Nothing)(implicit state: PickleState): Unit = throw new Error("IMPOSSIBLE")
    override def unpickle(implicit state: UnpickleState): Nothing        = throw new Error("IMPOSSIBLE")
  }
}
