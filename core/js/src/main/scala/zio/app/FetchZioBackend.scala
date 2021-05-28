package zio.app

import org.scalajs.dom.experimental.{BodyInit, Request => FetchRequest, Response => FetchResponse}
import sttp.capabilities.{Streams, WebSockets}
import sttp.client3.internal.ConvertFromFuture
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{AbstractFetchBackend, FetchOptions, SttpBackend}
import sttp.monad.{Canceler, MonadAsyncError}
import sttp.ws.{WebSocket, WebSocketClosed, WebSocketFrame}
import zio.stream._
import zio.{Canceler => _, _}

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.typedarray.{AB2TA, Int8Array}

trait ZioStreams extends Streams[ZioStreams] {
  override type BinaryStream = Stream[Throwable, Array[Byte]]
  override type Pipe[A, B]   = Stream[Throwable, A] => Stream[Throwable, B]
}

object ZioStreams extends ZioStreams

object ZioWebsockets {
  def compilePipe(
      ws: WebSocket[Task],
      pipe: Stream[Throwable, WebSocketFrame.Data[_]] => Stream[Throwable, WebSocketFrame]
  ): Task[Unit] = {
    Promise.make[Throwable, Unit].flatMap { wsClosed =>
      val onClose = Task(wsClosed.succeed(())).as(None)
      pipe(
        ZStream
          .repeatEffect(ws.receive().flatMap {
            case WebSocketFrame.Close(_, _)   => onClose
            case WebSocketFrame.Ping(payload) => ws.send(WebSocketFrame.Pong(payload)).as(None)
            case WebSocketFrame.Pong(_)       => Task.succeedNow(None)
            case in: WebSocketFrame.Data[_]   => Task.succeedNow(Some(in))
          })
          .catchSome { case _: WebSocketClosed => ZStream.fromEffect(onClose) }
          .interruptWhen(wsClosed)
          .flatMap {
            case None    => ZStream.empty
            case Some(f) => ZStream.succeed(f)
          }
      )
        .map(ws.send(_))
        .runDrain
        .ensuring(UIO(ws.close()))
    }
  }
}

class FetchZioBackend private (fetchOptions: FetchOptions, customizeRequest: FetchRequest => FetchRequest)
    extends AbstractFetchBackend[Task, ZioStreams, ZioStreams with WebSockets](
      fetchOptions,
      customizeRequest,
      ZioTaskMonadAsyncError
    ) {

  override val streams: ZioStreams = ZioStreams

  override protected def addCancelTimeoutHook[T](result: Task[T], cancel: () => Unit): Task[T] =
    result.ensuring(UIO(cancel()))

  override protected def handleStreamBody(s: Stream[Throwable, Array[Byte]]): Task[js.UndefOr[BodyInit]] = {
    // as no browsers support a ReadableStream request body yet we need to create an in memory array
    // see: https://stackoverflow.com/a/41222366/4094860
    val bytes = s.fold(Array.emptyByteArray) { case (data, item) => data ++ item }
    bytes.map(_.toTypedArray.asInstanceOf[BodyInit])
  }

  override protected def handleResponseAsStream(
      response: FetchResponse
  ): Task[(Stream[Throwable, Array[Byte]], () => Task[Unit])] = {
    Task {
      lazy val reader = response.body.getReader()
      val read        = ZIO.fromPromiseJS(reader.read())

      def go(): Stream[Throwable, Array[Byte]] =
        ZStream
          .fromEffect(read)
          .flatMap {
            ZStream.unfoldM(_) { chunk =>
              if (chunk.done) ZIO.none
              else read.map(next => Some(chunk.value -> next))
            }
          }
          .map(value => new Int8Array(value.buffer).toArray)

      val cancel = UIO(reader.cancel("Response body reader cancelled")).unit
      (go().ensuring(cancel), () => cancel)
    }
  }

  override protected def compileWebSocketPipe(
      ws: WebSocket[Task],
      pipe: Stream[Throwable, WebSocketFrame.Data[_]] => Stream[Throwable, WebSocketFrame]
  ): Task[Unit] =
    ZioWebsockets.compilePipe(ws, pipe)

  override implicit def convertFromFuture: ConvertFromFuture[Task] = new ConvertFromFuture[Task] {
    override def apply[T](f: Future[T]): Task[T] = Task.fromFuture(_ => f)
  }
}

object FetchZioBackend {
  def apply(
      fetchOptions: FetchOptions = FetchOptions.Default,
      customizeRequest: FetchRequest => FetchRequest = identity
  ): SttpBackend[Task, ZioStreams with WebSockets] =
    new FetchZioBackend(fetchOptions, customizeRequest)

  /** Create a stub backend for testing, which uses the [[Task]] response wrapper, and supports `Observable[ByteBuffer]`
    * streaming.
    *
    * See [[SttpBackendStub]] for details on how to configure stub responses.
    */
  def stub: SttpBackendStub[Task, ZioStreams] = SttpBackendStub(ZioTaskMonadAsyncError)
}

object ZioTaskMonadAsyncError extends MonadAsyncError[Task] {
  override def unit[T](t: T): Task[T] = Task.succeedNow(t)

  override def map[T, T2](fa: Task[T])(f: T => T2): Task[T2] = fa.map(f)

  override def flatMap[T, T2](fa: Task[T])(f: T => Task[T2]): Task[T2] =
    fa.flatMap(f)

  override def async[T](register: (Either[Throwable, T] => Unit) => Canceler): Task[T] =
    Task.effectAsync { cb =>
      val canceler = register {
        case Left(t)  => cb(ZIO.fail(t))
        case Right(t) => cb(ZIO.succeed(t))
      }
      Task(canceler.cancel())
    }

  override def error[T](t: Throwable): Task[T] = Task.fail(t)

  override protected def handleWrappedError[T](rt: Task[T])(h: PartialFunction[Throwable, Task[T]]): Task[T] =
    rt.catchSome(h)

  override def eval[T](t: => T): Task[T] = Task(t)

  override def suspend[T](t: => Task[T]): Task[T] = Task.effectSuspend(t)

  override def flatten[T](ffa: Task[Task[T]]): Task[T] = ffa.flatten

  override def ensure[T](f: Task[T], e: => Task[Unit]): Task[T] = f.ensuring(e.orDie)
}
