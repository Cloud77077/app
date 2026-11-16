package dev.nexterm

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AndroidSmokeTest {
 @get:Rule val app=createAndroidComposeRule<MainActivity>()

 @Test fun shellRendersColorAndEditorSavesUnicode() {
  app.onNodeWithText("Open terminal").performClick()
  app.onNodeWithText("Shell input").performTextInput("printf '\\033[31m%s_OK\\033[0m\\n' NEXTERM")
  app.onNodeWithText("Send").performClick()
  app.waitUntil(15000) { app.onAllNodesWithText("NEXTERM_OK",substring=true).fetchSemanticsNodes().isNotEmpty() }
  app.onNodeWithText("Files",useUnmergedTree=true).performClick()
  app.onNodeWithText("New name").performTextInput("smoke.txt")
  app.onNodeWithText("New file").performClick()
  app.onNodeWithText("smoke.txt").performClick()
  app.onAllNodes(hasSetTextAction()).onLast().performTextInput("hello λ 🌍")
  app.onNodeWithText("Save").performClick()
  assertEquals("hello λ 🌍",File(app.activity.filesDir,"workspace/smoke.txt").readText())
  app.onNodeWithText("Terminal",useUnmergedTree=true).performClick()
  app.onNodeWithText("Close",substring=false).performClick()
  app.onNodeWithText("Open terminal").assertExists()
 }

 @Test fun nativePtyUnicodeResizeInterruptAndExit() {
  val directory=File(app.activity.cacheDir,"pty-smoke").apply{mkdirs()}
  val h=requireNotNull(Pty.open(directory.absolutePath))
  fun send(text:String) {
   val bytes=text.toByteArray();var offset=0
   val deadline=System.currentTimeMillis()+10000
   while(offset < bytes.size && System.currentTimeMillis()<deadline) {
    val n=Pty.write(h[0],bytes.copyOfRange(offset,bytes.size));assertTrue(n>=0);offset+=n
    if(n==0)Thread.sleep(10)
   }
   assertEquals(bytes.size,offset)
  }
  val output=java.io.ByteArrayOutputStream()
  fun await(expected:String) {
   val deadline=System.currentTimeMillis()+15000
   while(System.currentTimeMillis()<deadline) {
    val bytes=Pty.read(h[0]) ?: break
    output.write(bytes)
    if(output.toString("UTF-8").contains(expected)) return
    Thread.sleep(10)
   }
   fail("Missing expected PTY output: $expected; received ${output.toString("UTF-8")}")
  }
  try {
   send("stty -echo\r");Thread.sleep(200);output.reset()
   Pty.resize(h[0],33,91)
   send("stty size; printf '\\316\\273\\360\\237\\214\\215\\n'\r")
   await("33 91");await("λ🌍")
   send("sleep 30\r");Thread.sleep(300);send("\u0003");send("printf 'INTERRUPT_OK\\n'\r");await("INTERRUPT_OK")
   send("exit\r")
   val deadline=System.currentTimeMillis()+10000
   var ended=false
   while(System.currentTimeMillis()<deadline) { if(Pty.read(h[0])==null) { ended=true;break };Thread.sleep(10) }
   assertTrue("Shell must exit",ended)
  } finally { Pty.close(h[0],h[1]);directory.deleteRecursively() }
 }
}
