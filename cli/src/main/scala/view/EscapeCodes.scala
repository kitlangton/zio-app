package view

import java.io.OutputStream

// TODO: Rewrite all this nonsense copy pasted from elsewhere :)
class EscapeCodes(out: OutputStream) {
  // Output an Escape Sequence
  private def ESC(command: Char): Unit = { out.write(("\u001b" + command).getBytes) }
  // Output a Control Sequence Inroducer
  private def CSI(sequence: String): Unit = {
    out.write(("\u001b[" + sequence).getBytes)
  }
  // Execute commands
  private def CSI(command: Char): Unit = { CSI(s"$command") }
  private def CSI(n: Int, command: Char): Unit = { CSI(s"$n$command") }
  private def CSI(n: Int, m: Int, command: Char): Unit = { CSI(s"$n;$m$command") }
  private def CSI(n: Int, m: Int, o: Int, command: Char): Unit = {
    CSI(s"$n;$m;$o$command")
  }
  // Execute commands in private modes
  private def CSI(mode: Char, command: Char): Unit = { CSI(s"$mode$command") }
  private def CSI(mode: Char, n: Int, command: Char): Unit = {
    CSI(s"$mode$n;$command")
  }

  /* DSR */
  def status(): Unit = { CSI(5, 'n') }

  // Cursor movement
  /* CUU */
  def moveUp(n: Int = 1): Unit = { CSI(n, 'A') }
  /* CUD */
  def moveDown(n: Int = 1): Unit = { CSI(n, 'B') }
  /* CUF */
  def moveRight(n: Int = 1): Unit = { CSI(n, 'C') }
  /* CUB */
  def moveLeft(n: Int = 1): Unit = { CSI(n, 'D') }
  /* CUP */
  def move(y: Int, x: Int): Unit = { CSI(x + 1, y + 1, 'H') }

  // Cursor management
  /* DECTCEM */
  def hideCursor(): Unit = { CSI('?', 25, 'l') }
  /* DECTCEM */
  def showCursor(): Unit = { CSI('?', 25, 'h') }
  /*  DECSC  */
  def saveCursor(): Unit = { ESC('7') }
  /*  DECRC  */
  def restoreCursor(): Unit = { ESC('8') }
  // Somehow this fails when the window has a height of 30-39:
  /*   CPR   */
  def cursorPosition(): (Int, Int) = {
    val r = getReport(() => CSI(6, 'n'), 2, 'R'); (r(1), r(0))
  }

  // Screen management
  /*   ED   */
  def clear(): Unit = { CSI(2, 'J') }
  /*   ED   */
  def clearToEnd(): Unit = { CSI(0, 'J') }
  /*   ED   */
  def clearLine(): Unit = { CSI(2, 'K') }
  /* DECSET */
  def alternateBuffer(): Unit = { CSI('?', 47, 'h') }
  /* DECRST */
  def normalBuffer(): Unit = { CSI('?', 47, 'l') }
  /*   RIS  */
  def fullReset(): Unit = { ESC('c') }
  /* dtterm */
  def resizeScreen(w: Int, h: Int): Unit = { CSI(8, w, h, 't') }
  /* dtterm */
  def screenSize(): (Int, Int) = {
    val r = getReport(() => CSI(18, 't'), 3, 't'); (r(2), r(1))
  }

  // Window management
  /* dtterm */
  def unminimizeWindow(): Unit = { CSI(1, 't') }
  /* dtterm */
  def minimizeWindow(): Unit = { CSI(2, 't') }
  /* dtterm */
  def moveWindow(x: Int, y: Int): Unit = { CSI(3, x, y, 't') }
  /* dtterm */
  def resizeWindow(w: Int, h: Int): Unit = { CSI(4, w, h, 't') }
  /* dtterm */
  def moveToTop(): Unit = { CSI(5, 't') }
  /* dtterm */
  def moveToBottom(): Unit = { CSI(6, 't') }
  /* dtterm */
  def restoreWindow(): Unit = { CSI(9, 0, 't') }
  /* dtterm */
  def maximizeWindow(): Unit = { CSI(9, 1, 't') }
  /* dtterm */
  def windowPosition(): (Int, Int) = {
    val r = getReport(() => CSI(13, 't'), 3, 't'); (r(2), r(1))
  }
  /* dtterm */
  def windowSize(): (Int, Int) = {
    val r = getReport(() => CSI(14, 't'), 3, 't'); (r(2), r(1))
  }

  // Color management
  /* ISO-8613-3 */
  def setForeground(color: Int): Unit = { CSI(38, 5, color, 'm') }
  /* ISO-8613-3 */
  def setBackground(color: Int): Unit = { CSI(48, 5, color, 'm') }
  /*     SGR    */
  def startBold(): Unit = { CSI(1, 'm') }
  /*     SGR    */
  def startUnderline(): Unit = { CSI(4, 'm') }
  /*     SGR    */
  def startBlink(): Unit = { CSI(5, 'm') }
  /*     SGR    */
  def startReverse(): Unit = { CSI(7, 'm') }
  /*     SGR    */
  def stopBold(): Unit = { CSI(22, 'm') }
  /*     SGR    */
  def stopUnderline(): Unit = { CSI(24, 'm') }
  /*     SGR    */
  def stopBlink(): Unit = { CSI(25, 'm') }
  /*     SGR    */
  def stopReverse(): Unit = { CSI(27, 'm') }
  /*     SGR    */
  def stopForeground(): Unit = { CSI(39, 'm') }
  /*     SGR    */
  def stopBackground(): Unit = { CSI(49, 'm') }
  /*     SGR    */
  def resetColors(): Unit = { CSI(0, 'm') }

  /** Executes a request and parses the response report.
    * Usually, they would start with a CSI but JLine seems to ignore them.
    * @param csi        CSI to execute
    * @param args       How many arguments are expected
    * @param terminator Terminator character of the report
    * @return           Sequence of parsed integers
    */
  def getReport(csi: () => Unit, args: Int, terminator: Char): Array[Int] = {
    // Send the CSI
    csi()
    out.flush()

    val results    = Array.fill(args)("")
    val separators = Array.fill(args - 1)(';') :+ terminator
    // Parse CSI
    System.in.read()
    System.in.read()
    // Parse each Ps
    for (i <- 0 until args) {
      var n = System.in.read()
      while (n != separators(i).toInt) {
        results(i) += n.toChar
        n = System.in.read()
      }
    }
    results.map(_.toInt)
  }
}
