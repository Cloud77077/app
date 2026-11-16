package dev.nexterm

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

@Composable fun ColumnScope.FileBrowser(root:File,onOpen:(File)->Unit,onError:(String)->Unit) {
 var directory by remember { mutableStateOf(root) }
 var refresh by remember { mutableIntStateOf(0) }
 var name by remember { mutableStateOf("") }
 var action by remember { mutableStateOf<File?>(null) }
 var moving by remember { mutableStateOf(false) }
 var destination by remember { mutableStateOf("") }
 var deleting by remember { mutableStateOf(false) }
 var query by remember { mutableStateOf("") }
 var searching by remember { mutableStateOf(false) }
 var matches by remember { mutableStateOf<List<WorkspaceFiles.Match>>(emptyList()) }
 var busy by remember { mutableStateOf(false) }
 val files=remember(directory,refresh) { directory.listFiles()?.sortedWith(compareBy<File>{!it.isDirectory}.thenBy{it.name})?:emptyList() }
 LaunchedEffect(query,refresh) {
  busy=true
  try { matches=withContext(Dispatchers.IO) { WorkspaceFiles.search(root,query) } }
  catch(e:CancellationException) { throw e }
  catch(e:Exception) { onError(e.message?:"Search failed") }
  finally { busy=false }
 }
 Text(directory.relativeTo(root).path.ifEmpty{"Workspace"})
 Row {
  if(directory!=root) TextButton(onClick={directory=directory.parentFile!!}) { Text("↑ Parent") }
  TextButton(onClick={refresh++}) { Text("Refresh") }
  TextButton(onClick={searching=!searching}) { Text("Search workspace") }
 }
 if(searching) {
  OutlinedTextField(query,{query=it},label={Text("Search file contents")},singleLine=true)
  Text(if(busy)"Searching…" else "${matches.size} matches (limit 200; files up to 1 MB)")
  LazyColumn(Modifier.weight(1f)) { items(matches) { match ->
   TextButton(onClick={onOpen(match.file)}) { Text("${match.file.relativeTo(root)}:${match.line}  ${match.preview}") }
  } }
 } else {
  OutlinedTextField(name,{name=it},label={Text("New name")},singleLine=true)
  Row { listOf(false,true).forEach { folder -> TextButton(onClick={try {
   WorkspaceFiles.create(directory,name,folder);name="";refresh++
  }catch(e:Exception){onError(e.message?:"Create failed")}}) { Text(if(folder)"New folder" else "New file") } } }
  LazyColumn(Modifier.weight(1f)) { items(files,key={it.absolutePath}) { file ->
   Row {
    TextButton(modifier=Modifier.weight(1f),onClick={try {
     require(file.canonicalFile.toPath().startsWith(root.canonicalFile.toPath())) { "Path is outside the workspace" }
     if(file.isDirectory) directory=file else onOpen(file)
    }catch(e:Exception){onError(e.message?:"Cannot open file")}
    }) { Text((if(file.isDirectory)"▸ " else "")+file.name) }
    TextButton(onClick={action=file}) { Text("⋮") }
   }
  } }
 }
 action?.let { file ->
  when {
   moving -> AlertDialog(onDismissRequest={moving=false;action=null},title={Text("Rename / move")},text={OutlinedTextField(destination,{destination=it},label={Text("Path relative to workspace")})},confirmButton={TextButton(onClick={try {
    WorkspaceFiles.move(root,file,destination);refresh++;action=null;moving=false
   }catch(e:Exception){onError(e.message?:"Move failed")}}){Text("Move")}},dismissButton={TextButton(onClick={moving=false;action=null}){Text("Cancel")}})
   deleting -> AlertDialog(onDismissRequest={deleting=false;action=null},title={Text("Delete ${file.name}?")},text={Text("This permanently removes the file or folder and its contents.")},confirmButton={TextButton(onClick={try {
    WorkspaceFiles.delete(root,file);refresh++;action=null;deleting=false
   }catch(e:Exception){onError(e.message?:"Delete failed")}}){Text("Delete")}},dismissButton={TextButton(onClick={deleting=false;action=null}){Text("Cancel")}})
   else -> AlertDialog(onDismissRequest={action=null},title={Text(file.name)},text={Column {
    TextButton(onClick={destination=file.relativeTo(root).path;moving=true}){Text("Rename / move")}
    TextButton(onClick={deleting=true}){Text("Delete")}
   }},confirmButton={TextButton(onClick={action=null}){Text("Close")}})
  }
 }
}
