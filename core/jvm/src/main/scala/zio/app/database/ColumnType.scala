package zio.app.database

/** TODO: Flesh out with [[java.sql.Types]]
  */
sealed trait ColumnType { self =>
  def renderSql: String = self match {
    case ColumnType.Int8          => "int4"
    case ColumnType.Int4          => "int8"
    case ColumnType.Int           => "int"
    case ColumnType.BigInt        => "bigint"
    case ColumnType.Serial        => "serial"
    case ColumnType.VarChar(size) => s"varchar($size)"
    case ColumnType.Text          => "text"
    case ColumnType.UUID          => "uuid"
    case ColumnType.Timestamp     => "timestamp"
  }
}

object ColumnType {
  case object Int4              extends ColumnType
  case object Int8              extends ColumnType
  case object Int               extends ColumnType
  case object BigInt            extends ColumnType
  case object Serial            extends ColumnType
  case class VarChar(size: Int) extends ColumnType
  case object Text              extends ColumnType
  case object UUID              extends ColumnType
  case object Timestamp         extends ColumnType

  def fromInfo(name: String, size: Int): ColumnType =
    name match {
      case "int4"      => Int4
      case "int8"      => Int8
      case "int"       => Int
      case "bigint"    => BigInt
      case "serial"    => Serial
      case "text"      => Text
      case "uuid"      => UUID
      case "timestamp" => Timestamp
      case "varchar"   => VarChar(size)
      case other       => throw new Error(s"missing column type ${other}")
    }

}
