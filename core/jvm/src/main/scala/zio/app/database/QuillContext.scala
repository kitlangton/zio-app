package zio.app.database

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.{PostgresZioJdbcContext, SnakeCase}
import zio.blocking.Blocking
import zio.{Has, ZLayer}

import javax.sql.DataSource

object QuillContext extends PostgresZioJdbcContext(SnakeCase) {
  val live: ZLayer[Blocking, Nothing, Has[DataSource]] =
    DataSourceLayer.fromPrefix("postgresDB").orDie
}
