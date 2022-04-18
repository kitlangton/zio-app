package database.ast

import zio.Chunk
import zio.app.database.ast.SQL.Constraint.{PrimaryKey, References}
import zio.app.database.ast.SQL.{Column, ColumnType, Constraint, CreateTable}
import zio.parser.internal.{PUnzippable, PZippable}
import zio.parser.{Regex, Syntax}

object SqlSyntax {
  val ident: Syntax[String, Char, Char, String, String] =
    Syntax
      .regex(Regex.filter(c => !c.isWhitespace && c != '(' && c != ')').atLeast(0), "OOPS")
      .transform(_.mkString, (str: String) => Chunk.fromIterable(str))

  val method: Syntax[String, Char, Char, String, String] =
    Syntax
      .regex(Regex.filter(c => !c.isWhitespace).atLeast(0), "OOPS")
      .transform(_.mkString, (str: String) => Chunk.fromIterable(str))

  // Constraint

  val primaryKey =
    (caseInsensitive("constraint") ~~ ident ~~ caseInsensitive("primary key"))
      .transform(PrimaryKey, (_: PrimaryKey).name)

  val references =
    (caseInsensitive("references") ~~ ident ~~ ident.parens)
      .transform({ case (a, b) => References(a, b) }, (ref: References) => (ref.name, ref.field))

  val default =
    (caseInsensitive("default") ~~ method)
      .transform(Constraint.Default, (_: Constraint.Default).method)

  val notNull =
    caseInsensitive("not null").as(Constraint.NotNull)

  val constraint: Syntax[String, Char, Char, Constraint, Constraint] =
    primaryKey.widen[Constraint] | references.widen[Constraint] | default.widen[Constraint] | notNull.widen[Constraint]

  // ColumnType

  def caseInsensitive(string: String): Syntax[String, Char, Char, Unit, Unit] =
    Syntax.string(string.toUpperCase, ()) | Syntax.string(string.toLowerCase, ())

  def caseInsensitive[A](string: String, value: A): Syntax[String, Char, Char, A, A] =
    Syntax.string(string.toUpperCase, value) | Syntax.string(string.toLowerCase, value)

  private val int: Syntax[String, Char, Char, Int, Int] = Syntax.digit.repeat.string
    .transform(_.toInt, (_: Int).toString)

  val varchar =
    (caseInsensitive("varchar") ~ Syntax.char('(') ~ int ~ Syntax.char(')')).transform(
      ColumnType.VarChar,
      (t: ColumnType.VarChar) => t.size
    )

  val uuid = caseInsensitive("uuid").as(ColumnType.UUID)

  val text = caseInsensitive("text").as(ColumnType.Text)

  val int32 = caseInsensitive("integer").as(ColumnType.Integer)

  val int64 = caseInsensitive("bigint").as(ColumnType.BigInt)

  val boolean = caseInsensitive("boolean").as(ColumnType.Boolean)

  val columnType: Syntax[String, Char, Char, ColumnType, ColumnType] =
    (varchar.widen[ColumnType] | uuid.widen[ColumnType] | text.widen[ColumnType] | int32.widen[ColumnType] | int64
      .widen[ColumnType] | boolean.widen[ColumnType])

  // Column

  val column: Syntax[String, Char, Char, Column, Column] =
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

  // CreateTable

  private val columns =
    column.repeatWithSep(Syntax.char(',').between(spaces, spaces1)).parens

  val createTable: Syntax[String, Char, Char, CreateTable, CreateTable] =
    (caseInsensitive("create table") ~~ ident ~~ columns ~ Syntax.char(';')).transform(
      { case (name, columns) => CreateTable(name, columns) },
      { case CreateTable(name, columns) => (name, columns) }
    )

  def main(args: Array[String]): Unit = {
    //
  }

  def roundTrip[A](a: A, syntax: Syntax[String, Char, Char, A, A]): Unit = {
    val value: Either[String, Chunk[Char]] = syntax.print(a)
    value match {
      case Left(err) =>
        println(s"Print Error: $err")
      case Right(chunk) =>
        syntax.parseString(chunk.mkString) match {
          case Left(err) =>
            println(s"Parse Error: $err")
          case Right(value) =>
            println("")
            println(s"Original: $a")
            println(s" Printed: ${Console.BLUE}${chunk.mkString}${Console.RESET}")
            println(s"  Parsed: ${Console.CYAN}$value${Console.RESET}")
        }
    }
  }

  implicit class SyntaxOps2[Value, Result](private val self: Syntax[String, Char, Char, Value, Result]) {

    /** Symbolic alias for zip */
    final def ~~[Value2, Result2, ZippedValue, ZippedResult](
        that: => Syntax[String, Char, Char, Value2, Result2]
    )(implicit
        unzippableValue: PUnzippable.In[Value, Value2, ZippedValue],
        zippableResult: PZippable.Out[Result, Result2, ZippedResult]
    ): Syntax[String, Char, Char, ZippedValue, ZippedResult] =
      self ~ (spaces1 ~ that)

    def parens: Syntax[String, Char, Char, Value, Result] = {
      self.surroundedBy(spaces).between(Syntax.char('('), Syntax.char(')')).surroundedBy(spaces)
    }

  }

  lazy val spaces1 = Syntax.regexDiscard(Regex.whitespace, "OOPs", Chunk(' '))

  // TODO: Syntax.whitespace.repeat0 fails
  lazy val spaces = Syntax.regexDiscard(Regex.whitespace, "OOPs", Chunk.empty)
}
