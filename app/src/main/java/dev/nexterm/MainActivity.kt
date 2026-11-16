package dev.nexterm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import java.io.File
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState);enableEdgeToEdge()
  val workspace=File(filesDir,"workspace").apply{mkdirs()}
  setContent {
   val prefs=getSharedPreferences("settings",MODE_PRIVATE)
   var useLight by remember { mutableStateOf(prefs.getBoolean("light",false)) }
   MaterialTheme(colorScheme=if(useLight) lightColorScheme() else darkColorScheme(primary=Color(0xff00b4ff), background=Color(0xff0a0a1a))) {
   val workspaceState:WorkspaceState=viewModel()
   var page by workspaceState::page
   var selected by remember { mutableStateOf<Session?>(Sessions.items.firstOrNull()) }
   var opened by workspaceState::document
   var pendingFile by remember { mutableStateOf<File?>(null) }
   val preferences=remember { getSharedPreferences("settings",MODE_PRIVATE) }
   var keepOn by remember { mutableStateOf(preferences.getBoolean("keepOn",false)) }
   LaunchedEffect(keepOn) { window.decorView.keepScreenOn=keepOn }
   pendingFile?.let { file -> AlertDialog(onDismissRequest={pendingFile=null},title={Text("Discard unsaved edits?")},text={Text("The current file has unsaved changes.")},confirmButton={TextButton(onClick={opened=EditorDocument(file);pendingFile=null;page="Editor"}){Text("Discard and open")}},dismissButton={TextButton(onClick={pendingFile=null}){Text("Cancel")}}) }

   var error by remember { mutableStateOf<String?>(null) }
   val importFile=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if(uri!=null) try {
     val bytes=contentResolver.openInputStream(uri)!!.use { input ->
      val result=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
      while(true) { val count=input.read(buffer);if(count<0)break;require(result.size()+count<=2_000_000){"Import limit is 2 MB"};result.write(buffer,0,count) }
      result.toByteArray()
     }
     val file=WorkspaceFiles.create(workspace,"import-${System.currentTimeMillis()}.txt",false)
     file.writeBytes(bytes)
     if(opened?.dirty==true) pendingFile=file else { opened=EditorDocument(file);page="Editor" }
    }catch(e:Exception){error=e.message}
   }
   var exportText by remember { mutableStateOf("") }
   val exportFile=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
    if(uri!=null) try { contentResolver.openOutputStream(uri)!!.use { it.write(exportText.toByteArray()) } }catch(e:Exception){error=e.message}
   }
   fun newSession() { try {
    val session=Sessions.create(workspace)
    try { startForegroundService(Intent(this@MainActivity,TerminalService::class.java));selected=session }
    catch(e:Exception) { Sessions.close(session);throw e }
   }catch(e:Exception){error=e.message} }
   var palette by remember { mutableStateOf(false) }
   if(palette) CommandPalette(preferences,{page=it},{newSession();page="Terminal"},{command ->
    if(selected?.running!=true) newSession()
    selected?.send(command+"\r");page="Terminal"
   },{palette=false})
   Scaffold(containerColor=MaterialTheme.colorScheme.background,bottomBar={NavigationBar { listOf("Terminal","Editor","Files","Git","Settings").forEach { label ->
    NavigationBarItem(selected=page==label,onClick={page=label},icon={Text(when(label){"Terminal"->">_";"Editor"->"{}";"Files"->"▤";"Git"->"⑂";else->"⚙"})},label={Text(label)})
   }}}) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).padding(12.dp).imePadding().onPreviewKeyEvent {
     if(it.type==KeyEventType.KeyDown && it.isCtrlPressed && it.isShiftPressed && it.key==Key.P) {palette=true;true} else false
    }) {
     Row { Text("NexTerm",Modifier.weight(1f),style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary)
      TextButton(onClick={palette=true}) { Text("Commands") }
     }
     Text("Android shell and workspace editor",style=MaterialTheme.typography.labelMedium)
     Spacer(Modifier.height(16.dp))
     if(page=="Files" || page=="Editor") Row {
      TextButton(onClick={importFile.launch(arrayOf("text/*","application/json","application/octet-stream"))}) { Text("Import file") }
      opened?.let { document -> TextButton(onClick={exportText=document.content;exportFile.launch(document.file.name)}) { Text("Export editor") } }
     }
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
        var terminalFont by remember { mutableFloatStateOf(preferences.getFloat("fontSize",13f)) }
        TerminalView(session,Modifier.weight(1f).fillMaxWidth(),terminalFont,{terminalFont=it;preferences.edit().putFloat("fontSize",it).apply()})
        session.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        Row(Modifier.horizontalScroll(rememberScrollState())) { listOf("Esc" to "\u001b","Tab" to "\t","Ctrl-C" to "\u0003","↑" to "\u001b[A","↓" to "\u001b[B","←" to "\u001b[D","→" to "\u001b[C","|" to "|","/" to "/","-" to "-","~" to "~","{" to "{","}" to "}",";" to ";").forEach{(label,key)->TextButton(onClick={session.send(key)}){Text(label)}} }
        var command by remember(session.id){mutableStateOf("")}
        var interactive by remember(session.id){mutableStateOf(false)}
        val keyboard=LocalSoftwareKeyboardController.current
        Row { Text("Interactive input");Switch(interactive,{interactive=it;command=""}) }
        fun submit() { session.send((if(interactive)"" else command)+"\r");command="";if(!interactive)keyboard?.hide() }
        Row { OutlinedTextField(command,{value ->
          if(interactive) {
           val prefix=command.commonPrefixWith(value).length
           session.send("\u007f".repeat(command.length-prefix)+value.substring(prefix))
          }
          command=value
         },Modifier.weight(1f).onPreviewKeyEvent { event ->
          if(event.type!=KeyEventType.KeyDown) false
          else if(event.isCtrlPressed) {
           val control=when(event.key){Key.C->"\u0003";Key.D->"\u0004";Key.Z->"\u001a";Key.L->"\u000c";Key.R->"\u0012";else->null}
           control?.let { session.send(it);command="";true } ?: false
          } else when(event.key) {
           Key.Enter -> {submit();true}
           Key.Backspace -> {
            if(interactive && command.isEmpty()) {session.send("\u007f");true} else false
           }
           Key.DirectionUp -> {session.send("\u001b[A");true}
           Key.DirectionDown -> {session.send("\u001b[B");true}
           else -> false
          }
         },label={Text(if(session.running)"Shell input" else "Session ended")},singleLine=true,enabled=session.running,
         keyboardOptions=KeyboardOptions(imeAction=ImeAction.Send,autoCorrectEnabled=false),keyboardActions=KeyboardActions(onSend={submit()}))
         Button(onClick={submit()},enabled=session.running){Text("Send")} }
       }
      }
      "Files" -> FileBrowser(workspace,{file ->
       if(opened?.dirty == true && opened?.file != file) pendingFile=file
       else { if(opened?.file != file) opened=EditorDocument(file);page="Editor" }
      },{error=it})
      "Editor" -> opened?.let { Editor(it,{error=it}) } ?: Text("Choose a file from Files to edit.")
      "Split" -> SplitWorkspace(selected,opened,{newSession()},{error=it})
      "Git" -> Text("Git requires a Linux userspace. Bootstrap integration is not yet implemented; Git operations are unavailable in this build.")
      "Settings" -> {
       Row { Text("Keep screen awake",Modifier.weight(1f));Switch(keepOn,{keepOn=it;preferences.edit().putBoolean("keepOn",it).apply()}) }
       Row { Text("Light theme",Modifier.weight(1f));Switch(useLight,{useLight=it;preferences.edit().putBoolean("light",it).apply()}) }
       Text("Workspace: ${workspace.absolutePath}")
       Text("Development build: Android system shell. Basic VT screen support; Linux packages and LSP are not available yet.")
      }
     }
    }
   }
  } }
 }
}
