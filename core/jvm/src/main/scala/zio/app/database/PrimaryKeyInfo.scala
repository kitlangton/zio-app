package zio.app.database

import zio.NonEmptyChunk
import zio.app.database.ResultSetSyntax.ResultSetOps

import java.sql.DatabaseMetaData

case class PrimaryKeyInfo(name: String, columns: NonEmptyChunk[String])

object PrimaryKeyInfo {
  def fromTable(metaData: DatabaseMetaData, tableName: String): Option[PrimaryKeyInfo] =
    metaData.getPrimaryKeys(null, null, tableName).map { rs =>
      val columnName = rs.getString("COLUMN_NAME")
      val keySeq     = rs.getString("KEY_SEQ")
      val pkName     = rs.getString("PK_NAME")
      (columnName, keySeq, pkName)
    } match {
      case chunk if chunk.isEmpty => None
      case (column, _, name) +: tail =>
        Some(tail.foldLeft(PrimaryKeyInfo(name, NonEmptyChunk(column))) { case (acc, (column, _, _)) =>
          acc.copy(columns = acc.columns :+ column)
        })
    }
}
