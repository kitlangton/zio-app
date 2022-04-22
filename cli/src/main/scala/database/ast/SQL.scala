package zio.app.database.ast

import database.ast.SqlSyntax
import zio.{Chunk, NonEmptyChunk}
import zio.app.database.ast.SQL.Constraint.PrimaryKey
import zio.app.database.ast.SQL._

sealed trait SQL extends Product with Serializable { self =>
  def render: String =
    SqlSyntax.statement.print(self).toOption.get.mkString
}

object SQL {
  // The entire SQL file
  final case class Document(
    statements: Chunk[SQL]
  ) extends SQL

  // CREATE TABLE
  final case class CreateTable(
    name: String,
    columns: Chunk[Column],
    ifNotExists: Boolean
  ) extends SQL

  // CREATE EXTENSION
  final case class CreateExtension(
    name: String,
    ifNotExists: Boolean
  ) extends SQL

  // DROP TABLE
  final case class DropTable(
    names: NonEmptyChunk[String],
    ifExists: Boolean
  ) extends SQL

  // ALTER TABLE
  //  - ADD COLUMN name type constraints
  //  - DROP COLUMN name
  //  - RENAME COLUMN name TO newName
  //  - ALTER COLUMN name TYPE type
  //  - ALTER COLUMN name SET DEFAULT value
  final case class AlterTable(
    name: String,
    actions: Chunk[AlterTable.Action],
    ifExists: Boolean
  ) extends SQL

  object AlterTable {
    sealed trait Action extends Product with Serializable

    object Action {
      final case class AddColumn(column: Column, ifNotExists: Boolean)                                  extends Action
      final case class DropColumn(name: String, ifExists: Boolean, cascade: Boolean, restrict: Boolean) extends Action
      final case class RenameColumn(name: String, newName: String)                                      extends Action
      final case class RenameConstraint(name: String, newName: String)                                  extends Action
      final case class RenameTable(newName: String)                                                     extends Action

      //    ALTER [ COLUMN ] column_name [ SET DATA ] TYPE data_type [ COLLATE collation ] [ USING expression ]
      final case class SetColumnType(name: String, dataType: String) extends Action
      //    ALTER [ COLUMN ] column_name SET DEFAULT expression
      final case class SetColumnDefault(name: String, expression: String) extends Action
      //    ALTER [ COLUMN ] column_name DROP DEFAULT
      final case class DropColumnDefault(name: String) extends Action
      //    ALTER [ COLUMN ] column_name { SET | DROP } NOT NULL
      final case class SetColumnNotNull(name: String, isNotNull: Boolean) extends Action
      //    ALTER [ COLUMN ] column_name DROP EXPRESSION [ IF EXISTS ]
      final case class DropColumnExpression(name: String, ifExists: Boolean) extends Action
      //    ALTER [ COLUMN ] column_name DROP IDENTITY [ IF EXISTS ]
      final case class DropColumnIdentity(name: String, ifExists: Boolean) extends Action

    }
  }

  // name TEXT NOT NULL
  final case class Column(
    name: String,
    columnType: SqlType,
    constraints: Chunk[Constraint]
  )

  sealed trait SqlType extends Product with Serializable

  object SqlType {
    final case class VarChar(size: Int)                  extends SqlType
    case object UUID                                     extends SqlType
    case object Text                                     extends SqlType
    case object Integer                                  extends SqlType
    case object BigInt                                   extends SqlType
    case object SmallInt                                 extends SqlType
    case object Serial                                   extends SqlType
    case object Boolean                                  extends SqlType
    final case class Decimal(precision: Int, scale: Int) extends SqlType
    case object Timestamp                                extends SqlType
    case object Date                                     extends SqlType
    case object Time                                     extends SqlType
    case object Json                                     extends SqlType
    case object Jsonb                                    extends SqlType
  }

  sealed trait Constraint extends Product with Serializable { self =>
    def isPrimaryKey: Boolean =
      self.isInstanceOf[PrimaryKey]
  }

  object Constraint {
    final case class PrimaryKey(name: Option[String])        extends Constraint
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
