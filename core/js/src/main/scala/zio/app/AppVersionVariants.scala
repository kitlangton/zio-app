package zio.app

import zio.app.internal.macros.Macros
import scala.language.experimental.macros

trait AppVersionVariants {
  def client[Service]: Service = macro Macros.client_impl[Service]
}
