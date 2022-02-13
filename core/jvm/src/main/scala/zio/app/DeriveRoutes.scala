package zio.app

import zhttp.http.HttpApp
import zio.app.internal.macros.Macros

import scala.language.experimental.macros

object DeriveRoutes {
  def gen[Service]: HttpApp[Service, Throwable] = macro Macros.routes_impl[Service]
}
