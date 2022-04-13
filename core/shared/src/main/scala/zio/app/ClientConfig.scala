package zio.app

final case class ClientConfig(authToken: Option[String])

object ClientConfig {
  val empty: ClientConfig =
    ClientConfig(None)
}
