package database.ast

import zio.Chunk
import zio.app.database.ast.SQL
import zio.app.database.ast.SQL.AlterTable.Action
import zio.app.database.ast.SQL.Constraint.{PrimaryKey, References}
import zio.app.database.ast.SQL.{AlterTable, Column, Constraint, CreateTable, SqlType}
import zio.parser.internal.{PUnzippable, PZippable}
import zio.parser.{Regex, Syntax}

object SqlSyntax {
  type StringSyntax[A] = Syntax[String, Char, Char, A, A]

  // # Utilities

  /**
   *   - not whitespace
   *   - does not contain parens
   */
  val ident: Syntax[String, Char, Char, String, String] =
    Syntax
      .filterChar(c => !c.isWhitespace && c != '(' && c != ')' && c != ';' && c != ',', "expected valid ident")
      .atLeast(0)
      .transform(_.mkString, (str: String) => Chunk.fromIterable(str))

  // currently used in DEFAULT method (e.g., DEFAULT now())
  val method: Syntax[String, Char, Char, String, String] =
    Syntax
      .filterChar(c => !c.isWhitespace && c != ',', "expected anything whitespace or comma")
      .atLeast(0)
      .transform(_.mkString, (str: String) => Chunk.fromIterable(str))

  // # Constraints

  val primaryKeyWithIdent =
    (caseInsensitive("constraint") ~~ ident ~~ caseInsensitive("primary key"))
      .transformEither(name => Right(PrimaryKey(Some(name))), (_: PrimaryKey).name.toRight("Missing Identifier"))

  // id uuid PRIMARY KEY
  // id uuid CONSTRAINT user_id PRIMARY KEY
  val primaryKey =
    caseInsensitive("primary key").as(Constraint.PrimaryKey(None))

  val references =
    (caseInsensitive("references") ~~ ident ~~ ident.parens)
      .transform({ case (a, b) => References(a, b) }, (ref: References) => (ref.name, ref.field))

  val default =
    (caseInsensitive("default") ~~ method)
      .transform(Constraint.Default, (_: Constraint.Default).method)

  val notNull =
    caseInsensitive("not null").as(Constraint.NotNull)

  val constraint: StringSyntax[Constraint] =
    primaryKeyWithIdent.widen[Constraint] | primaryKey.widen[Constraint] | references.widen[Constraint] |
      default.widen[Constraint] | notNull.widen[Constraint]

  // # SqlType

  def caseInsensitive(string: String): StringSyntax[Unit] =
    Syntax.string(string.toUpperCase, ()) | Syntax.string(string.toLowerCase, ())

  def caseInsensitive[A](string: String, value: A): StringSyntax[A] =
    Syntax.string(string.toUpperCase, value) | Syntax.string(string.toLowerCase, value)

  private val int: Syntax[String, Char, Char, Int, Int] = Syntax.digit.repeat.string
    .transform(_.toInt, (_: Int).toString)

  val varchar =
    (caseInsensitive("varchar") ~ int.parens)
      .transform(
        to = SqlType.VarChar,
        from = (t: SqlType.VarChar) => t.size
      )

  val uuid = caseInsensitive("uuid").as(SqlType.UUID)

  val text = caseInsensitive("text").as(SqlType.Text)

  val int32 = caseInsensitive("integer").as(SqlType.Integer)

  val int64 = caseInsensitive("bigint").as(SqlType.BigInt)

  val boolean = caseInsensitive("boolean").as(SqlType.Boolean)

  val columnType: StringSyntax[SqlType] =
    varchar.widen[SqlType] | uuid.widen[SqlType] | text.widen[SqlType] | int32.widen[SqlType] | int64
      .widen[SqlType] | boolean.widen[SqlType]

  // # Column

  val column: StringSyntax[Column] =
    (ident ~~ columnType ~ (spaces1 ~ constraint).repeat0)
      .transform(
        { case (name, dataType, constraints) =>
          Column(
            name,
            dataType,
            constraints
          )
        },
        { case Column(name, dataType, constraints) =>
          (name, dataType, constraints)
        }
      )

  // # CreateTable

  private val columns =
    column.repeatWithSep(Syntax.char(',').between(spaces, spaces1)).parens

  val ifNotExists: StringSyntax[Boolean] =
    (spaces1 ~ caseInsensitive("if not exists")).optional
      .transform[Boolean, Boolean](_.fold(false)(_ => true), if (_) Some(()) else None)

  val ifExists: StringSyntax[Boolean] =
    (spaces1 ~ caseInsensitive("if exists")).optional
      .transform[Boolean, Boolean](_.fold(false)(_ => true), if (_) Some(()) else None)

  val createTable: StringSyntax[CreateTable] =
    (caseInsensitive("create table") ~ ifNotExists ~~ ident ~~ columns ~ Syntax.char(';')).transform(
      { case (ifNotExists, name, columns) => CreateTable(name, columns, ifNotExists) },
      { case CreateTable(name, columns, ifNotExists) => (ifNotExists, name, columns) }
    )

  // # AlterTable

  // ADD [ COLUMN ] [ IF NOT EXISTS ] column_name data_type
  val addColumnAction: StringSyntax[Action.AddColumn] =
    (caseInsensitive("add column") ~ ifNotExists ~~ column)
      .transform(
        { case (ifNotExists, column) => AlterTable.Action.AddColumn(column, ifNotExists) },
        { case AlterTable.Action.AddColumn(column, ifNotExists) => (ifNotExists, column) }
      )

  // ALTER [ COLUMN ] column_name SET DEFAULT expression
  val alterColumnDefaultAction =
    (caseInsensitive("alter column") ~~ ident ~~ caseInsensitive("set default") ~~ method)
      .transform[AlterTable.Action.SetColumnDefault, AlterTable.Action.SetColumnDefault](
        { case (column, expression) => AlterTable.Action.SetColumnDefault(column, expression) },
        { case AlterTable.Action.SetColumnDefault(column, expression) => (column, expression) }
      )

  //  DROP COLUMN cruft
  //  DROP COLUMN IF EXISTS power

  val dropColumnAction: StringSyntax[AlterTable.Action.DropColumn] = {
    val cascade: StringSyntax[Boolean] =
      (spaces1 ~ caseInsensitive("cascade")).optional
        .transform[Boolean, Boolean](_.fold(false)(_ => true), if (_) Some(()) else None)

    val restrict: StringSyntax[Boolean] =
      (spaces1 ~ caseInsensitive("restrict")).optional
        .transform[Boolean, Boolean](_.fold(false)(_ => true), if (_) Some(()) else None)

    (caseInsensitive("drop column") ~ ifExists ~~ ident ~ cascade ~ restrict).transform(
      { case (ifExists, column, cascade, restrict) =>
        AlterTable.Action.DropColumn(column, ifExists, cascade, restrict)
      },
      { case AlterTable.Action.DropColumn(column, ifExists, cascade, restrict) =>
        (ifExists, column, cascade, restrict)
      }
    )
  }

  val setOrDropNotNullAction: StringSyntax[Action.SetColumnNotNull] = {
    val setOrDrop =
      caseInsensitive("set not null").as(true) |
        caseInsensitive("drop not null").as(false)

    // SET NOT NULL   -> true
    // DROP NOT NULL -> false

    (caseInsensitive("alter column") ~~ ident ~~ setOrDrop)
      .transform(
        { case (column, notNull) => AlterTable.Action.SetColumnNotNull(column, notNull) },
        { case AlterTable.Action.SetColumnNotNull(column, notNull) => (column, notNull) }
      )
  }

  val action: StringSyntax[Action] =
    addColumnAction.widen[AlterTable.Action] | dropColumnAction.widen[AlterTable.Action] |
      alterColumnDefaultAction.widen[AlterTable.Action] | setOrDropNotNullAction.widen[AlterTable.Action]

  // ALTER TABLE [ IF EXISTS ] name
  //    action [, ... ]
  val alterTable: StringSyntax[AlterTable] = {
    val actions =
      action.repeatWithSep(Syntax.char(',').between(spaces, spacesRender("\n  ")))

    (caseInsensitive("alter table") ~ ifExists ~~ ident ~ spacesRender("\n  ") ~ actions ~ Syntax.char(';')).transform(
      { case (ifExists, name, actions) => AlterTable(name, actions, ifExists) },
      { case AlterTable(name, actions, ifExists) => (ifExists, name, actions) }
    )
  }

  // # SQL Document

  val statement = createTable.widen[SQL] | alterTable.widen[SQL]

  val sqlDocument: StringSyntax[SQL.Document] = {
    val statements =
      statement.repeatWithSep(spacesNewLine).parens

    statements.transform(
      statements => SQL.Document(statements),
      { case SQL.Document(statements) => statements }
    )
  }

  // Syntax Extension Methods

  implicit class SyntaxOps2[Value, Result](private val self: Syntax[String, Char, Char, Value, Result]) extends AnyVal {

    /** Symbolic alias for zip */
    final def ~~[Value2, Result2, ZippedValue, ZippedResult](
      that: => Syntax[String, Char, Char, Value2, Result2]
    )(implicit
      unzippableValue: PUnzippable.In[Value, Value2, ZippedValue],
      zippableResult: PZippable.Out[Result, Result2, ZippedResult]
    ): Syntax[String, Char, Char, ZippedValue, ZippedResult] =
      self ~ (spaces1 ~ that)

    def parens: Syntax[String, Char, Char, Value, Result] =
      self
        .surroundedBy(spaces)
        .between(
          Syntax.char('('),
          Syntax.char(')')
        )
        .surroundedBy(spaces)

  }

  lazy val spaces1            = Syntax.regexDiscard(Regex.whitespace, "spaces1", Chunk(' '))
  lazy val spaces             = Syntax.regexDiscard(Regex.whitespace, "spaces", Chunk.empty)
  lazy val spacesNewLines     = Syntax.regexDiscard(Regex.whitespace, "spacesNewLine", Chunk('\n', '\n'))
  lazy val spacesNewLine      = Syntax.regexDiscard(Regex.whitespace, "spacesNewLine", Chunk('\n'))
  def spacesRender(s: String) = Syntax.regexDiscard(Regex.whitespace, "spacesRender", Chunk.fromIterable(s))
}
