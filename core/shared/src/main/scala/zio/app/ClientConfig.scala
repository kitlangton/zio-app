package zio.app

import sttp.model.Uri

final case class ClientConfig(
  authToken: Option[String],
  root: Uri.AbsolutePath
)

object ClientConfig {
  val empty: ClientConfig =
    ClientConfig(None, Uri.AbsolutePath(Seq.empty))
}
