package zio.app.database

import zio.Chunk

import java.util.UUID

object CodeGenSpike {
  trait HasColumnType[A] {
    def columnType: ColumnType
  }

  object HasColumnType {
    implicit val uuidType: HasColumnType[UUID] = new HasColumnType[UUID] {
      override def columnType: ColumnType = ColumnType.UUID
    }

    implicit val stringType: HasColumnType[String] = new HasColumnType[String] {
      override def columnType: ColumnType = ColumnType.Text
    }

    implicit val intType: HasColumnType[Int] = new HasColumnType[Int] {
      override def columnType: ColumnType = ColumnType.Int
    }

    implicit val longType: HasColumnType[Long] = new HasColumnType[Long] {
      override def columnType: ColumnType = ColumnType.BigInt
    }
  }

  case class Table(name: String, columns: Chunk[Column]) {
    def primaryKey: Option[Column] =
      columns.find(_.isPrimaryKey)
  }
  case class Column(name: String, columnType: ColumnType, isPrimaryKey: Boolean = false)

  def table(name: String)(columns: Column*): Table = Table(name, Chunk.fromIterable(columns))

  def column[T](name: String)(implicit ct: HasColumnType[T]): Column = Column(name, ct.columnType)

  def primaryKey[T](name: String)(implicit ct: HasColumnType[T]): Column =
    Column(name, ct.columnType, isPrimaryKey = true)
  def primaryKey[T](implicit ct: HasColumnType[T]): Column =
    Column("id", ct.columnType, isPrimaryKey = true)

  def references(table: Table): Column =
    table.primaryKey match {
      case Some(pk) =>
        Column(s"fk_${table.name}_${pk.name}", pk.columnType)
      case None =>
        throw new Error(s"${table.name} has no primary key.")
    }

  lazy val user: Table =
    table("user")(
      primaryKey[UUID],
      column[Long]("name"),
      column[Int]("age")
    )

  lazy val post: Table =
    table("post")(
      primaryKey[UUID],
      column[String]("title"),
      references(user)
    )

  def main(args: Array[String]): Unit = {
    println(user, post)
  }
}
