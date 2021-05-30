package zio.app.database

import org.postgresql.Driver
import zio.app.database.Migration.render
import zio.app.database.ResultSetSyntax.ResultSetOps

import java.sql.DriverManager

object PostgresService {
  DriverManager.registerDriver(new Driver())

  def tableInfo: List[TableInfo] = {
    val url      = "jdbc:postgresql://localhost/kit";
    val conn     = DriverManager.getConnection(url, "postgres", "");
    val metaData = conn.getMetaData

    val tables = metaData
      .getTables(null, null, null, Array("TABLE"))
      .map(TableInfo.fromResultSet(_, metaData))

    println(tables.toList.mkString("\n\n"))

    tables.toList
  }

  def main(args: Array[String]): Unit = {
    tableInfo.foreach { ti =>
      val mod    = Migration.CreateTable(ti)
      val string = render(mod)
      println()
      println(string)
      println()
    }
  }
}
