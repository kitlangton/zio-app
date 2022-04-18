package zio.app.database.ast

import database.ast.SqlSyntax
import zio.Chunk
import zio.app.database.ast.SQL.Constraint.PrimaryKey
import zio.app.database.ast.SQL._

sealed trait SQL extends Product with Serializable { self =>
  def render: String = self match {
    case table: CreateTable =>
      SqlSyntax.createTable.print(table).toOption.get.mkString
  }
}

object SQL {
  // CREATE TABLE
  // create table question
  //(
  //    id       uuid default gen_random_uuid() not null
  //        constraint question_pk
  //            primary key,
  //    question text                           not null,
  //    author   varchar(255)                   not null
  //);
  final case class CreateTable(
      name: String,
      columns: Chunk[Column]
  ) extends SQL

  final case class Column(
      name: String,
      columnType: ColumnType,
      constraints: Chunk[Constraint]
  )

  sealed trait ColumnType extends Product with Serializable

  object ColumnType {
    final case class VarChar(size: Int) extends ColumnType

    case object UUID extends ColumnType

    case object Text extends ColumnType

    case object Integer extends ColumnType

    case object BigInt extends ColumnType

    case object Boolean extends ColumnType
  }

  sealed trait Constraint extends Product with Serializable { self =>
    def isPrimaryKey: Boolean =
      self.isInstanceOf[PrimaryKey]
  }

  object Constraint {
    final case class PrimaryKey(name: String)                extends Constraint
    final case class References(name: String, field: String) extends Constraint
    final case class Default(method: String)                 extends Constraint
    case object NotNull                                      extends Constraint
  }

  sealed trait AlterTableCommand extends Product with Serializable

  object AlterTableCommand {
    // Add
    // Drop
    // Rename

    final case class Add(column: Column)                      extends AlterTableCommand
    final case class Drop(columnName: String)                 extends AlterTableCommand
    final case class Rename(oldName: String, newName: String) extends AlterTableCommand
    final case class Alter(column: Column)                    extends AlterTableCommand

  }

}
