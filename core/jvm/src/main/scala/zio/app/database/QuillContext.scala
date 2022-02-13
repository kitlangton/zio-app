package zio.app.database

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.{PostgresZioJdbcContext, SnakeCase}
import zio.ZLayer

import javax.sql.DataSource

object QuillContext extends PostgresZioJdbcContext(SnakeCase) {
  val live: ZLayer[Any, Nothing, DataSource] =
    DataSourceLayer.fromPrefix("postgresDB").orDie
}
