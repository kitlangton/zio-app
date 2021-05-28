package zio.app.database

import java.sql.ResultSet

case class ColumnInfo(name: String, columnType: ColumnType)

object ColumnInfo {
  def fromResultSet(resultSet: ResultSet): ColumnInfo = {
    val columnName     = resultSet.getString("COLUMN_NAME")
    val columnTypeName = resultSet.getString("TYPE_NAME")
    val size           = resultSet.getInt("COLUMN_SIZE")
    ColumnInfo(columnName, ColumnType.fromInfo(columnTypeName, size))
  }
}
