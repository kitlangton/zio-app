package zio.app.database

import org.postgresql.Driver
import zio.app.database.DBModification.render

import java.sql.DriverManager
import scala.collection.mutable.ListBuffer



object PostgresService {
  DriverManager.registerDriver(new Driver())


  def tableInfo: List[TableInfo] = {
    val url      = "jdbc:postgresql://localhost/kit";
    val conn     = DriverManager.getConnection(url, "postgres", "");
    val metaData = conn.getMetaData
    val result   = metaData.getTables(null, null, null, Array("TABLE"))

    val tables = ListBuffer.empty[TableInfo]

    while (result.next()) {
      tables += TableInfo.fromResultSet(result, metaData)
    }

    tables.toList
  }

  def main(args: Array[String]): Unit = {
    tableInfo.foreach { ti =>
      val mod    = DBModification.CreateTable(ti)
      val string = render(mod)
      println()
      println(string)
      println()
    }
  }
}
