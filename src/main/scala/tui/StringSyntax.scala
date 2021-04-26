package tui

object StringSyntax {
  val ansiRegex: String = "(\u009b|\u001b\\[)[0-?]*[ -\\/]*[@-~]".r.regex

  implicit final class StringOps(val self: String) extends AnyVal {
    def removingAnsiCodes: String =
      self.replaceAll(ansiRegex, "")
  }
}
