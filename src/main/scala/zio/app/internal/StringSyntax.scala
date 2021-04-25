package zio.app.internal

object StringSyntax {
  val ansiRegex = "(\u009b|\u001b\\[)[0-?]*[ -\\/]*[@-~]".r.regex

  implicit class StringOps(val self: String) extends AnyVal {
    def removingAnsiCodes: String =
      self.replaceAll(ansiRegex, "")
  }
}
