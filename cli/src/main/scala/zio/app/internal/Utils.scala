package zio.app.internal

import zio._
import zio.process.Command

object Utils {
  def launchBrowser(url: String): Unit = {
    import java.awt.Desktop
    import java.net.URI;

    if (Desktop.isDesktopSupported && Desktop.getDesktop.isSupported(Desktop.Action.BROWSE)) {
      Desktop.getDesktop.browse(new URI("http://localhost:3000"));
    }
  }

  def say(message: String): UIO[Unit] =
    Command("say", message).run.ignore
}
