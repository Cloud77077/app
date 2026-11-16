package dev.nexterm

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Two live panes; wide windows use columns, narrow windows stack the panes. */
@Composable fun SplitWorkspace(session:Session?,document:EditorDocument?,onNewShell:()->Unit,onError:(String)->Unit) {
 BoxWithConstraints(Modifier.fillMaxSize()) {
  if(maxWidth>=600.dp) Row(Modifier.fillMaxSize()) {
   Column(Modifier.weight(1f).fillMaxHeight()) { SplitTerminal(session,onNewShell) }
   VerticalDivider()
   Column(Modifier.weight(1f).fillMaxHeight()) { document?.let { Editor(it,onError) } ?: Text("Open a file from Files, then return to Split.") }
  } else Column(Modifier.fillMaxSize()) {
   Column(Modifier.weight(1f).fillMaxWidth()) { SplitTerminal(session,onNewShell) }
   HorizontalDivider()
   Column(Modifier.weight(1f).fillMaxWidth()) { document?.let { Editor(it,onError) } ?: Text("Open a file from Files, then return to Split.") }
  }
 }
}
@Composable private fun ColumnScope.SplitTerminal(session:Session?,onNewShell:()->Unit) {
 if(session==null) Button(onClick=onNewShell) { Text("Open terminal") }
 else {
  Text(session.name)
  TerminalView(session,Modifier.weight(1f).fillMaxWidth())
  var command by remember(session.id) { mutableStateOf("") }
  Row {
   OutlinedTextField(command,{command=it},Modifier.weight(1f),singleLine=true,label={Text("Shell input")},enabled=session.running)
   Button(onClick={session.send(command+"\r");command=""},enabled=session.running) { Text("Send") }
  }
 }
}
