package dev.nexterm

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.json.JSONArray
import org.json.JSONObject

@Composable fun CommandPalette(preferences:SharedPreferences,onNavigate:(String)->Unit,onNewShell:()->Unit,onRun:(String)->Unit,onClose:()->Unit) {
 var filter by remember { mutableStateOf("") }
 var label by remember { mutableStateOf("") }
 var command by remember { mutableStateOf("") }
 var snippets by remember { mutableStateOf(runCatching {
  val array=JSONArray(preferences.getString("snippets","[]"))
  (0 until array.length()).map { array.getJSONObject(it).let { item -> item.getString("name") to item.getString("command") } }
 }.getOrDefault(emptyList())) }
 fun store(items:List<Pair<String,String>>) {
  snippets=items
  val array=JSONArray();items.forEach { (name,text) -> array.put(JSONObject().put("name",name).put("command",text)) }
  preferences.edit().putString("snippets",array.toString()).apply()
 }
 AlertDialog(onDismissRequest=onClose,title={Text("Commands and snippets")},text={
  Column {
   OutlinedTextField(filter,{filter=it},singleLine=true,label={Text("Filter commands")})
   LazyColumn(Modifier.weight(1f,fill=false)) {
    item { if("new terminal".contains(filter,true)) TextButton(onClick={onNewShell();onClose()}) { Text("New terminal") } }
    items(listOf("Terminal","Editor","Files","Split","Settings").filter { it.contains(filter,true) }) { page ->
     TextButton(onClick={onNavigate(page);onClose()}) { Text("Open $page") }
    }
    items(snippets.filter { it.first.contains(filter,true) }) { snippet ->
     Row {
      TextButton(modifier=Modifier.weight(1f),onClick={onRun(snippet.second);onClose()}) { Text("Run: ${snippet.first}") }
      TextButton(onClick={store(snippets-snippet)}) { Text("Remove") }
     }
    }
   }
   OutlinedTextField(label,{label=it},singleLine=true,label={Text("Snippet name")})
   OutlinedTextField(command,{command=it},label={Text("Shell command")})
   TextButton(enabled=label.isNotBlank() && command.isNotBlank(),onClick={store((snippets.filterNot { it.first==label } + (label to command)).takeLast(50));label="";command=""}) { Text("Save snippet") }
  }
 },confirmButton={TextButton(onClick=onClose) { Text("Close") }})
}
