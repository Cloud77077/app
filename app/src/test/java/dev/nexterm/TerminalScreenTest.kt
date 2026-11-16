package dev.nexterm

import org.junit.Assert.*
import org.junit.Test

class TerminalScreenTest {
 @Test fun carriageReturnAndEraseReplaceProgress() {
  val s=TerminalScreen(3,20);s.feed("progress 100%\r\u001b[Kdone")
  assertEquals("done",s.text())
 }
 @Test fun fragmentedSequencesAndColors() {
  val s=TerminalScreen(3,20);s.feed("\u001b[3");s.feed("1;1mRED\u001b[0m plain")
  assertEquals("RED plain",s.text());assertEquals(1,s.lines()[0][0].foreground)
  assertTrue(s.lines()[0][0].bold);assertFalse(s.lines()[0][3].bold)
 }
 @Test fun cursorAddressingAndEraseScreen() {
  val s=TerminalScreen(3,10);s.feed("old\u001b[2J\u001b[2;3Hnew")
  assertEquals("\n  new",s.text());assertEquals(1,s.row);assertEquals(5,s.column)
 }
 @Test fun alternateScreenRestoresPrimaryAndDoesNotLeakHistory() {
  val s=TerminalScreen(2,10);s.feed("shell\u001b[?1049happ\r\na\r\nb\u001b[?1049l!")
  assertEquals("shell!",s.text());assertFalse(s.alternate)
 }
 @Test fun historyBoundAndScrollRegion() {
  val s=TerminalScreen(2,10,3)
  repeat(20) { s.feed("$it\r\n") }
  assertEquals(5,s.lines().size)
  s.feed("\u001b[3J");assertEquals(2,s.lines().size)
 }
 @Test fun deviceStatusAndResize() {
  val s=TerminalScreen();var reply="";s.reply={reply=it}
  s.feed("\u001b[3;5H\u001b[6n");assertEquals("\u001b[3;5R",reply)
  s.resize(2,3);assertEquals(1,s.row);assertEquals(2,s.column)
  s.clearHistory();s.feed("\u001b[?1049h");s.resize(4,8);s.feed("\u001b[?1049l")
  assertEquals(4,s.lines().size);assertEquals(8,s.lines()[0].size)
 }
 @Test fun oscTitlesAndUnknownControlsAreConsumed() {
  val s=TerminalScreen();s.feed("a\u001b]0;secret title\u001b");s.feed("\\b\u001b[?2004hc")
  assertEquals("abc",s.text())
 }
 @Test fun delayedWrapAndSurrogateAcrossReads() {
  val s=TerminalScreen(2,3);s.feed("abc\rX");assertEquals("Xbc",s.text())
  s.feed("\u001b[2;1H\ud83c");s.feed("\udf0d");assertTrue(s.text().endsWith("🌍"))
 }
 @Test fun trueColorAndIndexedColor() {
  val s=TerminalScreen();s.feed("\u001b[38;2;12;34;56mX\u001b[48;5;123mY")
  assertEquals(0x10c2238,s.lines()[0][0].foreground);assertEquals(123,s.lines()[0][1].background)
 }
}
