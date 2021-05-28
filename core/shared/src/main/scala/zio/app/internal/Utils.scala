package zio.app.internal

import boopickle.Default._
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.{HttpHeaderNames, HttpHeaderValues}
import zhttp.http._
import zio._
import zio.clock.Clock
import zio.duration.durationInt
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

  def makeRouteStream[R, E, A: Pickler, B: Pickler](
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
          val stream = call(unpickled).mapConcatChunk { a =>
            Chunk.fromByteBuffer(Pickle.intoBytes(a))
          }

          Response.http(content = HttpData.fromStream(stream))

        case _ => Response.ok
      }
    }
  }

  def makeRouteNullaryStream[R, E, A: Pickler](
      service: String,
      method: String,
      call: ZStream[R, E, A]
  ): HttpApp[R, E] = {
    val service0 = service
    val method0  = method
    Http.collect { case Method.GET -> Root / `service0` / `method0` =>
      val stream: ZStream[R, E, Byte] = call
        .mapConcatChunk { a =>
          Chunk.fromByteBuffer(Pickle.intoBytes(a))
        }
//        .tap { b =>
//          UIO(println(s"BYTE ${b}"))
//        }
//        .map { b =>
//          println(s"BYTE")
//          b
//        }

//      val that: ZStream[R, E, Byte] = ZStream.fromSchedule(Schedule.spaced(100.millis)).mapConcatChunk(_ => Chunk(1.toByte)).provideLayer(Clock.live)
      Response.http(content = HttpData.fromStream(stream))
    }

  }

}
