package zio.app.database

import zio.{Chunk, ChunkBuilder}

import java.sql.ResultSet

object ResultSetSyntax {
  implicit final class ResultSetOps(val resultSet: ResultSet) extends AnyVal {
    def map[A](f: ResultSet => A): Chunk[A] = {
      val builder = ChunkBuilder.make[A](resultSet.getFetchSize)
      while (resultSet.next()) {
        builder += f(resultSet)
      }
      builder.result()
    }
  }
}
