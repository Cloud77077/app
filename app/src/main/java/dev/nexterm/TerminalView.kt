package dev.nexterm

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val palette = listOf(0xff101018,0xffe06c75,0xff98c379,0xffe5c07b,0xff61afef,0xffc678dd,0xff56b6c2,0xffdbe7f5,
 0xff657080,0xffff8080,0xffb6e890,0xffffdc90,0xff91caff,0xffe2a0ff,0xff88e0e8,0xffffffff).map { Color(it) }
private fun terminalColor(code:Int):Color = when {
 code >= 0x1000000 -> Color(0xff000000 or (code.toLong() and 0xffffff))
 code < 16 -> palette[code.coerceAtLeast(0)]
 code >= 232 -> (8+(code-232)*10).let { Color(it,it,it) }
 else -> { val n=code-16; val levels=listOf(0,95,135,175,215,255); Color(levels[n/36],levels[n/6%6],levels[n%6]) }
}

@Composable fun TerminalView(session:Session,modifier:Modifier=Modifier,fontSize:Float=13f,onFontSize:(Float)->Unit={}) {
 val revision=session.revision
 val screen=session.screen
 val lines=remember(session,revision) { screen.lines() }
 val list=rememberLazyListState()
 val measurer=rememberTextMeasurer()
 val style=TextStyle(fontFamily=FontFamily.Monospace,fontSize=fontSize.sp,lineHeight=(fontSize*1.3f).sp)
 val metrics=measurer.measure("M",style)
 BoxWithConstraints(modifier.background(palette[0]).pointerInput(fontSize) {
  detectTransformGestures { _,_,zoom,_ -> if(zoom != 1f) onFontSize((fontSize*zoom).coerceIn(8f,28f)) }
 }) {
  val columns=(constraints.maxWidth/metrics.size.width.coerceAtLeast(1)).coerceIn(2,300)
  val rows=(constraints.maxHeight/metrics.size.height.coerceAtLeast(1)).coerceIn(2,200)
  LaunchedEffect(session,rows,columns) { session.resize(rows,columns) }
  var previousCount by remember(session) { mutableIntStateOf(0) }
  LaunchedEffect(revision) {
   val atEnd=(list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1)>=previousCount-1
   if(previousCount==0 || atEnd || screen.alternate) list.scrollToItem((lines.size-1).coerceAtLeast(0))
   previousCount=lines.size
  }
  SelectionContainer {
   LazyColumn(state=list,modifier=Modifier.fillMaxSize()) {
    itemsIndexed(lines) { index,line ->
     val content=buildAnnotatedString {
      line.forEachIndexed { x,cell ->
       val cursor=screen.cursorVisible && session.running && index==lines.size-screen.rows+screen.row && x==screen.column
       withStyle(SpanStyle(color=terminalColor(if(cursor)cell.background else cell.foreground),background=terminalColor(if(cursor)cell.foreground else cell.background),fontWeight=if(cell.bold)FontWeight.Bold else FontWeight.Normal)) { append(cell.text) }
      }
     }
     Text(content,style=style,softWrap=false,maxLines=1)
    }
   }
  }
 }
}
