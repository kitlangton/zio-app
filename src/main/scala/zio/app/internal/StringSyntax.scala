package zio.app.internal

object StringSyntax {
  implicit class StringOps(val self: String) extends AnyVal {
    def removingAnsiCodes: String =
      self.replaceAll("\u001B\\[[;\\d]*m", "")
  }
}
