package zio.app

import zhttp.http.HttpApp
import zio.Has
import zio.app.internal.macros.Macros

import scala.language.experimental.macros

object App {
  def routes[Service]: HttpApp[Has[Service], Throwable] = macro Macros.routes_impl[Service]
}
