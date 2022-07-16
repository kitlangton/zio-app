//package database.ast
//
//import zio.Chunk
//import zio.app.database.ast.SQL
//import zio.app.database.ast.SQL.Constraint.PrimaryKey
//import zio.app.database.ast.SQL.{Column, Constraint, CreateTable, SqlType}
//import zio.parser.Syntax
//import zio.test._
//
//object SqlParsingSpec extends ZIOSpecDefault {
//
//  def spec =
//    suite("SqlParsingSpec")(
//      columnSuite,
//      createTableSuite,
//      alterTableSuite,
//    )
//
//  def assertSyntaxEquality[A](string: String, value: A)(
//    syntax: Syntax[String, Char, Char, A, A],
//  )(implicit trace: ZTraceElement): Assert = {
//    val parsed  = syntax.parseString(string)
//    val printed = syntax.print(value).map(_.mkString)
//    assertTrue(
//      printed.toOption.get.trim == string,
//      parsed.toOption.get == value,
//    )
//  }
//
//  def assertSyntaxEquality[A](value: A)(
//    syntax: Syntax[String, Char, Char, A, A],
//  )(implicit trace: ZTraceElement): Assert = {
//    val printed = syntax.print(value).map(_.mkString)
//    println(trace)
//    println(printed)
//    val parsed = syntax.parseString(printed.toOption.get.trim)
//    assertTrue(
//      value == parsed.toOption.get,
//    )
//  }
//
//  // Fixtures
//
//  lazy val columnSuite =
//    suite("Column")(
//      test("idColumn") {
//        assertSyntaxEquality(
//          "id VARCHAR(255) DEFAULT gen_random_uuid() NOT NULL CONSTRAINT question_pk PRIMARY KEY",
//          Column(
//            name = "id",
//            columnType = SqlType.VarChar(255),
//            constraints = Chunk(
//              Constraint.Default("gen_random_uuid()"),
//              Constraint.NotNull,
//              Constraint.PrimaryKey(Some("question_pk")),
//            ),
//          ),
//        )(SqlSyntax.column)
//      },
//      test("nameColumn") {
//        assertSyntaxEquality(
//          "name VARCHAR(255)",
//          Column(
//            name = "name",
//            columnType = SqlType.VarChar(255),
//            constraints = Chunk(),
//          ),
//        )(SqlSyntax.column)
//      },
//      test("primary key without name") {
//        assertSyntaxEquality(
//          "name TEXT PRIMARY KEY",
//          Column(
//            name = "name",
//            columnType = SqlType.Text,
//            constraints = Chunk(
//              PrimaryKey(None),
//            ),
//          ),
//        )(SqlSyntax.column)
//      },
//    )
//
//  lazy val createTableSuite =
//    suite("CREATE TABLE")(
//      test("user table") {
//        assertSyntaxEquality(
//          "CREATE TABLE user (id VARCHAR(255) DEFAULT gen_random_uuid() NOT NULL CONSTRAINT question_pk PRIMARY KEY, name VARCHAR(255));",
//          CreateTable(
//            "user",
//            Chunk(
//              Column(
//                name = "id",
//                columnType = SqlType.VarChar(255),
//                constraints = Chunk(
//                  Constraint.Default("gen_random_uuid()"),
//                  Constraint.NotNull,
//                  Constraint.PrimaryKey(Some("question_pk")),
//                ),
//              ),
//              Column(
//                name = "name",
//                columnType = SqlType.VarChar(255),
//                constraints = Chunk(
//                ),
//              ),
//            ),
//            ifNotExists = false,
//          ),
//        )(SqlSyntax.createTable)
//      },
//      test("IF NOT EXISTS") {
//        assertSyntaxEquality(
//          "CREATE TABLE IF NOT EXISTS user (id UUID);",
//          CreateTable(
//            name = "user",
//            columns = Chunk(
//              Column("id", SqlType.UUID, Chunk()),
//            ),
//            ifNotExists = true,
//          ),
//        )(SqlSyntax.createTable)
//      },
//      test("parsing") {
//        val input =
//          """
//CREATE TABLE question(
//    id       uuid default gen_random_uuid() NOT NULL
//        CONSTRAINT question_pk
//            PRIMARY KEY,
//    question TEXT                           NOT NULL,
//    author   VARCHAR(255)                   NOT NULL
//);
//""".trim
//
//        val result = SqlSyntax.createTable.parseString(input)
//        val createTable =
//          CreateTable(
//            name = "question",
//            columns = Chunk(
//              Column(
//                name = "id",
//                columnType = SqlType.UUID,
//                constraints = Chunk(
//                  Constraint.Default("gen_random_uuid()"),
//                  Constraint.NotNull,
//                  PrimaryKey(
//                    name = Some("question_pk"),
//                  ),
//                ),
//              ),
//              Column(
//                name = "question",
//                columnType = SqlType.Text,
//                constraints = Chunk(
//                  Constraint.NotNull,
//                ),
//              ),
//              Column(
//                name = "author",
//                columnType = SqlType.VarChar(size = 255),
//                constraints = Chunk(
//                  Constraint.NotNull,
//                ),
//              ),
//            ),
//            ifNotExists = false,
//          )
//
//        val printed = SqlSyntax.createTable.print(createTable).map(_.mkString)
//
//        val expected =
//          "CREATE TABLE question (id UUID DEFAULT gen_random_uuid() NOT NULL CONSTRAINT question_pk PRIMARY KEY, question TEXT NOT NULL, author VARCHAR(255) NOT NULL);"
//
//        assertTrue(
//          result.toOption.get == createTable,
//          printed.toOption.get == expected,
//        )
//      },
//    )
//
//  lazy val alterTableSuite =
//    suite("ALTER TABLE")(
//      suite("ADD COLUMN")(
//        test("simple") {
//          assertSyntaxEquality(
//            """
//ALTER TABLE user
//  ADD COLUMN name VARCHAR(255);
//""".trim,
//            SQL.AlterTable(
//              name = "user",
//              actions = Chunk(
//                SQL.AlterTable.Action.AddColumn(
//                  column = Column(
//                    name = "name",
//                    columnType = SqlType.VarChar(255),
//                    constraints = Chunk(),
//                  ),
//                  ifNotExists = false,
//                ),
//              ),
//              ifExists = false,
//            ),
//          )(SqlSyntax.alterTable)
//        },
//        test("multiple columns") {
//          assertSyntaxEquality(
//            """
//ALTER TABLE user
//  ADD COLUMN name VARCHAR(255),
//  ADD COLUMN IF NOT EXISTS age INTEGER,
//  ALTER COLUMN id SET DEFAULT gen_random_uuid(),
//  ALTER COLUMN name SET NOT NULL,
//  ALTER COLUMN name DROP NOT NULL,
//  DROP COLUMN cruft,
//  DROP COLUMN IF EXISTS power,
//  DROP COLUMN IF EXISTS power CASCADE,
//  DROP COLUMN power RESTRICT;
//""".trim,
//            SQL.AlterTable(
//              name = "user",
//              actions = Chunk(
//                SQL.AlterTable.Action.AddColumn(
//                  column = Column(
//                    name = "name",
//                    columnType = SqlType.VarChar(255),
//                    constraints = Chunk(),
//                  ),
//                  ifNotExists = false,
//                ),
//                SQL.AlterTable.Action.AddColumn(
//                  column = Column(
//                    name = "age",
//                    columnType = SqlType.Integer,
//                    constraints = Chunk(),
//                  ),
//                  ifNotExists = true,
//                ),
//                SQL.AlterTable.Action.SetColumnDefault(
//                  name = "id",
//                  expression = "gen_random_uuid()",
//                ),
//                SQL.AlterTable.Action.SetColumnNotNull(
//                  name = "name",
//                  isNotNull = true,
//                ),
//                SQL.AlterTable.Action.SetColumnNotNull(
//                  name = "name",
//                  isNotNull = false,
//                ),
//                SQL.AlterTable.Action.DropColumn(
//                  name = "cruft",
//                  ifExists = false,
//                  cascade = false,
//                  restrict = false,
//                ),
//                SQL.AlterTable.Action.DropColumn(
//                  name = "power",
//                  ifExists = true,
//                  cascade = false,
//                  restrict = false,
//                ),
//                SQL.AlterTable.Action.DropColumn(
//                  name = "power",
//                  ifExists = true,
//                  cascade = true,
//                  restrict = false,
//                ),
//                SQL.AlterTable.Action.DropColumn(
//                  name = "power",
//                  ifExists = false,
//                  cascade = false,
//                  restrict = true,
//                ),
//              ),
//              ifExists = false,
//            ),
//          )(SqlSyntax.alterTable)
//        },
//      ),
//    )
//}
//
//object TabularExample {
//
//  val example =
//    """
//CREATE TABLE question(
//    id       uuid default gen_random_uuid() NOT NULL
//        CONSTRAINT question_pk
//            PRIMARY KEY,
//    question TEXT                           NOT NULL,
//    author   VARCHAR(255)                   NOT NULL,
//    author_id uuid                         REFERENCES user(id),
//    age INTEGER
//);
//""".trim
//
//  def main(args: Array[String]): Unit = {
//    val sql = SqlSyntax.createTable.parseString(example).toOption.get
//    println(sql)
//  }
//
//}
