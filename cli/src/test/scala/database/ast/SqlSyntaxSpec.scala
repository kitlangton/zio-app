package database.ast

import zio.app.database.ast.SQL.Constraint.PrimaryKey
import zio.{Chunk, ZTraceElement}
import zio.app.database.ast.SQL.{Column, ColumnType, Constraint, CreateTable}
import zio.parser.Syntax
import zio.test._

import java.util.UUID

object SqlParsingSpec extends ZIOSpecDefault {

  def spec =
    suite("SqlParsingSpec")(
      suite("Column")(
        test("column 1") {
          checkBoth(
            idColumn,
            "id VARCHAR(255) DEFAULT gen_random_uuid() NOT NULL CONSTRAINT question_pk PRIMARY KEY"
          )(SqlSyntax.column)
        },
        test("column 2") {
          checkBoth(
            nameColumn,
            "name VARCHAR(30)"
          )(SqlSyntax.column)
        }
      ),
      suite("CreateTable")(
        test("first") {
          checkBoth(
            createTable,
            "CREATE TABLE user (id VARCHAR(255) DEFAULT gen_random_uuid() NOT NULL CONSTRAINT question_pk PRIMARY KEY, name VARCHAR(30));"
          )(SqlSyntax.createTable)
        },
        test("parsing") {
          val input =
            """
CREATE TABLE question(
    id       uuid default gen_random_uuid() NOT NULL
        CONSTRAINT question_pk
            PRIMARY KEY,
    question TEXT                           NOT NULL,
    author   VARCHAR(255)                   NOT NULL
);
""".trim

          val result = SqlSyntax.createTable.parseString(input)
          val createTable =
            CreateTable(
              name = "question",
              columns = Chunk(
                Column(
                  name = "id",
                  columnType = ColumnType.UUID,
                  constraints = Chunk(
                    Constraint.Default("gen_random_uuid()"),
                    Constraint.NotNull,
                    PrimaryKey(
                      name = "question_pk"
                    )
                  )
                ),
                Column(
                  name = "question",
                  columnType = ColumnType.Text,
                  constraints = Chunk(
                    Constraint.NotNull
                  )
                ),
                Column(
                  name = "author",
                  columnType = ColumnType.VarChar(size = 255),
                  constraints = Chunk(
                    Constraint.NotNull
                  )
                )
              )
            )

          val printed = SqlSyntax.createTable.print(createTable).map(_.mkString)

          val expected =
            "CREATE TABLE question (id UUID DEFAULT gen_random_uuid() NOT NULL CONSTRAINT question_pk PRIMARY KEY, question TEXT NOT NULL, author VARCHAR(255) NOT NULL);"

          assertTrue(
            result.toOption.get == createTable,
            printed.toOption.get == expected
          )
        }
      )
    )

  def checkBoth[A](value: A, string: String)(
      syntax: Syntax[String, Char, Char, A, A]
  )(implicit trace: ZTraceElement): Assert = {
    val parsed  = syntax.parseString(string)
    val printed = syntax.print(value).map(_.mkString)
    assertTrue(
      printed.toOption.get.trim == string,
      parsed.toOption.get == value
    )
  }

  // Fixtures

  lazy val idColumn =
    Column(
      name = "id",
      columnType = ColumnType.VarChar(255),
      constraints = Chunk(
        Constraint.Default("gen_random_uuid()"),
        Constraint.NotNull,
        Constraint.PrimaryKey("question_pk")
      )
    )

  lazy val nameColumn =
    Column(
      name = "name",
      columnType = ColumnType.VarChar(30),
      constraints = Chunk(
      )
    )

  lazy val createTable =
    CreateTable(
      "user",
      Chunk(
        idColumn,
        nameColumn
      )
    )
}

object TabularExample {

  val example =
    """
CREATE TABLE question(
    id       uuid default gen_random_uuid() NOT NULL
        CONSTRAINT question_pk
            PRIMARY KEY,
    question TEXT                           NOT NULL,
    author   VARCHAR(255)                   NOT NULL,
    author_id uuid                         REFERENCES user(id),
    age INTEGER
);
""".trim

  def main(args: Array[String]): Unit = {
    val sql = SqlSyntax.createTable.parseString(example).toOption.get
    println(sql)
  }

}
