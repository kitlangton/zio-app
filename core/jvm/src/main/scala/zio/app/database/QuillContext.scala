package zio.app.database

import io.getquill.context.ZioJdbc.QDataSource
import io.getquill.{PostgresZioJdbcContext, SnakeCase}
import zio.blocking.Blocking
import zio.{Has, ZLayer}

import java.sql.Connection

object QuillContext extends PostgresZioJdbcContext(SnakeCase) {
  val live: ZLayer[Blocking, Nothing, Has[Connection]] =
    (QDataSource.fromPrefix("postgresDB") >>> QDataSource.toConnection).orDie
}
