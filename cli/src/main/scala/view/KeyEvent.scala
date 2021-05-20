package view

import zio.Chunk

sealed trait KeyEvent

object KeyEvent {
  case class UnSupported(bytes: Chunk[Int]) extends KeyEvent
  case class Character(char: Char)          extends KeyEvent
  case object Up                            extends KeyEvent
  case object Down                          extends KeyEvent
  case object Left                          extends KeyEvent
  case object Right                         extends KeyEvent
  case object Enter                         extends KeyEvent
  case object Delete                        extends KeyEvent
  case object Escape                        extends KeyEvent
  case object Tab                           extends KeyEvent
  case object ShiftTab                      extends KeyEvent
  case object Exit                          extends KeyEvent
}
