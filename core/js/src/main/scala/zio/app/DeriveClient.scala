package zio.app

import zio.app.internal.macros.Macros
import scala.language.experimental.macros

object DeriveClient {
  def gen[Service]: Service = macro Macros.client_impl[Service]
}
