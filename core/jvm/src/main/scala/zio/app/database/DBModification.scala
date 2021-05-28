package zio.app.database

sealed trait DBModification

object DBModification {
  case class CreateTable(tableInfo: TableInfo)                          extends DBModification
  case class DropTable(tableName: String)                               extends DBModification
  case class AddColumn(tableName: String, columnInfo: ColumnInfo)       extends DBModification
  case class AlterColumnType(tableName: String, columnInfo: ColumnInfo) extends DBModification

  def render(mod: DBModification): String = mod match {
    case CreateTable(tableInfo) =>
      val columns = tableInfo.columnInfo.map { columnInfo =>
        s"${columnInfo.name} ${columnInfo.columnType.renderSql}"
      }

      s"""
create table ${tableInfo.name} (
  ${columns.mkString(",\n  ")}
);
         """.trim

    case DropTable(tableName)                   => ""
    case AddColumn(tableName, columnInfo)       => ""
    case AlterColumnType(tableName, columnInfo) => ""
  }

  def diff(old: List[TableInfo], incoming: List[TableInfo]): List[DBModification] = ???
}
