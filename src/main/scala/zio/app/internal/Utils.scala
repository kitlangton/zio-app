package zio.app.internal

/** - Make g8 for zio-slides
  * - Create method for running g8 command
  * - Clean-up rendering
  */

object Utils {
  def launchBrowser(url: String): Unit = {
    import java.awt.Desktop
    import java.net.URI;

    if (Desktop.isDesktopSupported && Desktop.getDesktop.isSupported(Desktop.Action.BROWSE)) {
      Desktop.getDesktop.browse(new URI("http://localhost:3000"));
    }
  }
}
