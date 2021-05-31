package zio.app.database

import zio.{Chunk, NonEmptyChunk}

sealed trait Migration

object Migration {
  case class CreateTable(tableInfo: TableInfo)                          extends Migration
  case class DropTable(tableName: String)                               extends Migration
  case class AddColumn(tableName: String, columnInfo: ColumnInfo)       extends Migration
  case class AlterColumnType(tableName: String, columnInfo: ColumnInfo) extends Migration

  def render(mod: Migration): String = mod match {
    case CreateTable(tableInfo) =>
      val columns: Chunk[String] = tableInfo.columnInfo.map { columnInfo =>
        s"${columnInfo.name} ${columnInfo.columnType.renderSql}"
      }

      val primaryKey = tableInfo.primaryKeyInfo
        .map { pk =>
          Chunk(s"CONSTRAINT ${pk.name} PRIMARY KEY ${pk.columns.mkString("(", ", ", ")")}")
        }
        .getOrElse(Chunk.empty)

      val foreignKeys = tableInfo.foreignKeys
        .map { fk =>
          val (selfColumns, parentColumns) = fk.relations.unzip

          val onDelete = (fk.deleteRule match {
            case DeleteRule.NoAction   => None
            case DeleteRule.Cascade    => Some("CASCADE")
            case DeleteRule.SetNull    => Some("SET NULL")
            case DeleteRule.SetDefault => Some("SET DEFAULT")
            case DeleteRule.Restrict   => Some("RESTRICT")
          }).map { rule => s" ON DELETE $rule" }.getOrElse("")

          s"CONSTRAINT ${fk.foreignKeyName.getOrElse("")} FOREIGN KEY ${selfColumns
            .mkString("(", ", ", ")")} REFERENCES ${fk.parentKeyTable} ${parentColumns.mkString("(", ", ", ")")}$onDelete"
        }

      val info = columns ++ primaryKey ++ foreignKeys

      s"""
CREATE TABLE ${tableInfo.name} (
  ${info.mkString(",\n  ")}
);
         """.trim

    case DropTable(tableName)                   => ""
    case AddColumn(tableName, columnInfo)       => ""
    case AlterColumnType(tableName, columnInfo) => ""
  }

  def diff(old: List[TableInfo], incoming: List[TableInfo]): List[Migration] = ???
}
