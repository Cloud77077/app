package dev.nexterm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState);enableEdgeToEdge()
  val workspace=File(filesDir,"workspace").apply{mkdirs()}
  setContent { MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xff00b4ff), background=Color(0xff0a0a1a))) {
   var page by remember { mutableStateOf("Terminal") }
   var selected by remember { mutableStateOf<Session?>(Sessions.items.firstOrNull()) }
   var opened by remember { mutableStateOf<File?>(null) }
   var error by remember { mutableStateOf<String?>(null) }
   fun newSession() { try {
    startForegroundService(Intent(this@MainActivity,TerminalService::class.java))
    selected=Sessions.create(workspace)
   }catch(e:Exception){error=e.message} }
   Scaffold(containerColor=Color.Transparent,bottomBar={NavigationBar { listOf("Terminal","Editor","Files","Git","Settings").forEach { label ->
    NavigationBarItem(selected=page==label,onClick={page=label},icon={Text(when(label){"Terminal"->">_";"Editor"->"{}";"Files"->"▤";"Git"->"⑂";else->"⚙"})},label={Text(label)})
   }}}) { padding ->
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xff0a0a1a),Color(0xff0d0d2b)))).padding(padding).padding(12.dp).imePadding()) {
     Text("NexTerm",style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary)
     Text("Your pocket Linux workstation",style=MaterialTheme.typography.labelMedium)
     Spacer(Modifier.height(16.dp))
     error?.let { Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick={error=null}){Text("Dismiss")} }
     when(page){
      "Terminal" -> {
       Row(Modifier.horizontalScroll(rememberScrollState())) {
        Sessions.items.forEach { s -> TextButton(onClick={selected=s}){Text(if(s==selected)"● ${s.name}" else s.name)} }
        TextButton(onClick={newSession()}){Text("+ New")}
       }
       val session=selected
       if(session==null) { Text("Start a shell in your private workspace.");Button(onClick={newSession()}){Text("Open terminal")} }
       else {
        var title by remember(session.id) { mutableStateOf(session.name) }
        Row { OutlinedTextField(title,{title=it;session.name=it},Modifier.weight(1f),singleLine=true,label={Text("Session name")})
         TextButton(onClick={Sessions.close(session);selected=Sessions.items.firstOrNull();if(Sessions.items.isEmpty())stopService(Intent(this@MainActivity,TerminalService::class.java))}){Text("Close")}
        }
        val scroll=rememberScrollState()
        LaunchedEffect(session.output){scroll.scrollTo(scroll.maxValue)}
        SelectionContainer(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).background(Color.White.copy(alpha=.06f)).padding(8.dp)) {
         Text(session.output,fontFamily=FontFamily.Monospace,fontSize=13.sp,color=Color(0xffdbe7f5))
        }
        Row(Modifier.horizontalScroll(rememberScrollState())) { listOf("Esc" to "\u001b","Tab" to "\t","Ctrl-C" to "\u0003","↑" to "\u001b[A","↓" to "\u001b[B","←" to "\u001b[D","→" to "\u001b[C","|" to "|","/" to "/","-" to "-","~" to "~","{" to "{","}" to "}",";" to ";").forEach{(label,key)->TextButton(onClick={session.send(key)}){Text(label)}} }
        var command by remember(session.id){mutableStateOf("")}
        Row { OutlinedTextField(command,{command=it},Modifier.weight(1f),label={Text(if(session.running)"Shell input" else "Session ended")},singleLine=true)
         Button(onClick={session.send(command+"\r");command=""},enabled=session.running){Text("Send")} }
       }
      }
      "Files" -> FileBrowser(workspace,{opened=it;page="Editor"},{error=it})
      "Editor" -> opened?.let { Editor(it,{error=it}) } ?: Text("Choose a file from Files to edit.")
      "Git" -> Text("Git requires a Linux userspace. Bootstrap integration is not yet implemented; Git operations are unavailable in this build.")
      "Settings" -> {
       var keepOn by remember { mutableStateOf(false) }
       Row { Text("Keep screen awake",Modifier.weight(1f));Switch(keepOn,{keepOn=it;window.decorView.keepScreenOn=it}) }
       Text("Workspace: ${workspace.absolutePath}")
       Text("Development build: Android system shell. Linux packages, VT rendering and LSP are not available yet.")
      }
     }
    }
   }
  } }
 }
}
@Composable private fun FileBrowser(root:File,onOpen:(File)->Unit,onError:(String)->Unit) {
 var directory by remember { mutableStateOf(root) }
 var refresh by remember { mutableIntStateOf(0) }
 var name by remember { mutableStateOf("") }
 val files=remember(directory,refresh){directory.listFiles()?.sortedWith(compareBy<File>{!it.isDirectory}.thenBy{it.name})?:emptyList()}
 Text(directory.relativeTo(root).path.ifEmpty{"Workspace"})
 if(directory!=root)TextButton(onClick={directory=directory.parentFile!!}){Text("↑ Parent")}
 Row { OutlinedTextField(name,{name=it},Modifier.weight(1f),label={Text("New name")},singleLine=true)
  listOf(false,true).forEach { folder -> TextButton(onClick={try {
   WorkspaceFiles.create(directory,name,folder);name="";refresh++
  }catch(e:Exception){onError(e.message?:"Failed")}}){Text(if(folder)"Folder" else "File")} }
 }
 Column(Modifier.verticalScroll(rememberScrollState())) {files.forEach{file->
  TextButton(onClick={if(file.isDirectory)directory=file else onOpen(file)}){Text((if(file.isDirectory)"▸ " else "  ")+file.name)}
 }}
}
@Composable private fun Editor(file:File,onError:(String)->Unit) {
 val loaded = remember(file) {runCatching{require(file.length()<2_000_000){"File too large"};file.readText()}}
 if(loaded.isFailure){ Text(loaded.exceptionOrNull()?.message ?: "Cannot read file");return }
 var content by remember(file) {mutableStateOf(loaded.getOrThrow())}
 var saved by remember(file){mutableStateOf(content)}
 Column { Row {Text(file.name+if(content!=saved)" ●" else "",Modifier.weight(1f));Button(onClick={try{WorkspaceFiles.save(file,content);saved=content}catch(e:Exception){onError(e.message?:"Save failed")}}){Text("Save")}}
 OutlinedTextField(content,{content=it},Modifier.fillMaxSize(),textStyle=LocalTextStyle.current.copy(fontFamily=FontFamily.Monospace,fontSize=14.sp)) }
}
