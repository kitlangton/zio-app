package zio.app.database

sealed trait ColumnType { self =>
  def renderSql: String = self match {
    case ColumnType.Int8          => "int4"
    case ColumnType.Int4          => "int8"
    case ColumnType.Serial        => "serial"
    case ColumnType.VarChar(size) => s"varchar($size)"
    case ColumnType.Text          => "text"
  }
}

object ColumnType {
  case object Int8              extends ColumnType
  case object Int4              extends ColumnType
  case object Serial            extends ColumnType
  case class VarChar(size: Int) extends ColumnType
  case object Text              extends ColumnType

  def fromInfo(name: String, size: Int): ColumnType =
    name match {
      case "int4"    => Int4
      case "int8"    => Int8
      case "serial"  => Serial
      case "text"    => Text
      case "varchar" => VarChar(size)
    }
}
