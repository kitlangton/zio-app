package zio.app.database

import zio.Chunk

import java.sql.{DatabaseMetaData, ResultSet}

case class TableInfo(
    name: String,
    columnInfo: Chunk[ColumnInfo],
    primaryKeyInfo: Option[PrimaryKeyInfo],
    foreignKeys: Chunk[ForeignKeyInfo],
    indices: Chunk[IndexInfo]
)

object TableInfo {
  def fromResultSet(resultSet: ResultSet, metaData: DatabaseMetaData): TableInfo = {
    val tableName = resultSet.getString("TABLE_NAME")

    val columns     = ColumnInfo.fromTable(metaData, tableName)
    val primaryKey  = PrimaryKeyInfo.fromTable(metaData, tableName)
    val foreignKeys = ForeignKeyInfo.fromTable(metaData, tableName)
    val indices     = IndexInfo.fromTable(metaData, tableName)

    val info = TableInfo(tableName, columns, primaryKey, foreignKeys, indices)
    println(info.indices)
    info
  }
}
