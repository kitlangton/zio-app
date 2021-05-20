package zio.app

import zio.app.internal.macros.Macros
import scala.language.experimental.macros

object App {
  def client[Service]: Service = macro Macros.client_impl[Service]
}
