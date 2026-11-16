package dev.nexterm

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import java.io.File
import androidx.lifecycle.ViewModel
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

class WorkspaceState:ViewModel() {
 var document by mutableStateOf<EditorDocument?>(null)
 var page by mutableStateOf("Terminal")
}

class EditorDocument(val file:File) {
 private val loaded=runCatching { require(file.length()<2_000_000) { "File too large" };file.readText() }
 val loadError=loaded.exceptionOrNull()?.message
 var content by mutableStateOf(loaded.getOrDefault("")); private set
 private var saved by mutableStateOf(content)
 private val undo=ArrayDeque<String>()
 private val redo=ArrayDeque<String>()
 val dirty get()=content!=saved
 val canUndo get()=undo.isNotEmpty()
 val canRedo get()=redo.isNotEmpty()
 fun edit(text:String) { if(text!=content) { undo.addLast(content);while(undo.size>100 || undo.sumOf { it.length.toLong() }>4_000_000)undo.removeFirst();redo.clear();content=text } }
 fun undo() { if(undo.isNotEmpty()) { redo.addLast(content);content=undo.removeLast() } }
 fun redo() { if(redo.isNotEmpty()) { undo.addLast(content);content=redo.removeLast() } }
 fun save() { WorkspaceFiles.save(file,content);saved=content }
}

@Composable fun Editor(document:EditorDocument,onError:(String)->Unit) {
 if(document.loadError!=null) { Text(document.loadError);return }
 var find by remember { mutableStateOf("") }
 var replacement by remember { mutableStateOf("") }
 var searching by remember { mutableStateOf(false) }
 val keyboard=LocalSoftwareKeyboardController.current
 fun save() { try { document.save();keyboard?.hide() } catch(e:Exception) { onError(e.message?:"Save failed") } }
 Column(Modifier.onPreviewKeyEvent { event ->
  if(event.type==KeyEventType.KeyDown && event.isCtrlPressed) when(event.key) {
   Key.S -> { save();true }
   Key.Z -> { if(event.isShiftPressed)document.redo() else document.undo();true }
   Key.Y -> { document.redo();true }
   Key.F -> { searching=true;true }
   else -> false
  } else false
 }) {
  Text(document.file.name+if(document.dirty)" ●" else "")
  Row(Modifier.horizontalScroll(rememberScrollState())) {
   Button(onClick={save()}) { Text("Save") }
   TextButton(onClick={document.undo()},enabled=document.canUndo) { Text("Undo") }
   TextButton(onClick={document.redo()},enabled=document.canRedo) { Text("Redo") }
   TextButton(onClick={searching=!searching}) { Text("Find / replace") }
  }
  if(searching) {
   OutlinedTextField(find,{find=it},singleLine=true,label={Text("Find (case sensitive)")})
   OutlinedTextField(replacement,{replacement=it},singleLine=true,label={Text("Replace with")})
   Row {
    Text(if(find.isEmpty())"Enter text" else "${Regex(Regex.escape(find)).findAll(document.content).count()} matches")
    TextButton(onClick={document.edit(document.content.replace(find,replacement))},enabled=find.isNotEmpty()) { Text("Replace all") }
   }
  }
  Text("${document.content.count { it=='\n' }+1} lines · ${document.content.length} characters")
  OutlinedTextField(document.content,{document.edit(it)},Modifier.weight(1f).fillMaxWidth(),textStyle=LocalTextStyle.current.copy(fontFamily=FontFamily.Monospace,fontSize=14.sp))
 }
}
