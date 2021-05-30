package zio.app.internal

sealed trait ZioResponse[+E, +A]

object ZioResponse {
  def succeed[A](value: A): ZioResponse[Nothing, A]            = Succeed(value)
  def fail[E](value: E): ZioResponse[E, Nothing]               = Fail(value)
  def die(throwable: Throwable): ZioResponse[Nothing, Nothing] = Die(throwable)
  def interrupt(fiberId: Long): ZioResponse[Nothing, Nothing]  = Interrupt(fiberId: Long)

  case class Succeed[A](value: A)      extends ZioResponse[Nothing, A]
  case class Fail[E](value: E)         extends ZioResponse[E, Nothing]
  case class Die(throwable: Throwable) extends ZioResponse[Nothing, Nothing]
  case class Interrupt(fiberId: Long)  extends ZioResponse[Nothing, Nothing]
}
