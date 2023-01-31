package zio.app

import zio.http.HttpApp
import zio.app.internal.macros.Macros

import scala.language.experimental.macros

object DeriveRoutes {
  def gen[Service]: HttpApp[Service, Throwable] = macro Macros.routes_impl[Service]
}
