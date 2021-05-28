package zio.app.database

import java.sql.{DatabaseMetaData, ResultSet}
import scala.collection.mutable.ListBuffer

case class TableInfo(name: String, columnInfo: List[ColumnInfo])

object TableInfo {
  def fromResultSet(resultSet: ResultSet, metaData: DatabaseMetaData): TableInfo = {
    val tableName       = resultSet.getString("TABLE_NAME")
    val columnResultSet = metaData.getColumns(null, null, tableName, null)
    val columns         = ListBuffer.empty[ColumnInfo]
    while (columnResultSet.next()) {
      columns += ColumnInfo.fromResultSet(columnResultSet)
    }
    TableInfo(tableName, columns.toList)
  }
}
