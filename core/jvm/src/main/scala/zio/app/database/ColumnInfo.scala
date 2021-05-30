package zio.app.database

import zio.Chunk
import zio.app.database.ResultSetSyntax.ResultSetOps

import java.sql.DatabaseMetaData

case class ColumnInfo(name: String, columnType: ColumnType, default: Option[String], isNullable: Boolean)

object ColumnInfo {
  def fromTable(metaData: DatabaseMetaData, tableName: String): Chunk[ColumnInfo] =
    metaData.getColumns(null, null, tableName, null).map { resultSet =>
      val columnName     = resultSet.getString("COLUMN_NAME")
      val columnTypeName = resultSet.getString("TYPE_NAME")
      val size           = resultSet.getInt("COLUMN_SIZE")
      val default        = Option(resultSet.getString("COLUMN_DEF"))
      val isNullable     = resultSet.getBoolean("IS_NULLABLE")
      ColumnInfo(columnName, ColumnType.fromInfo(columnTypeName, size), default, isNullable)
    }
}
