package dev.nexterm
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
class Session(val id: Int, directory: File) {
 var name by mutableStateOf("Shell $id")
 var output by mutableStateOf("")
 var running by mutableStateOf(true)
 private val handle = requireNotNull(Pty.open(directory.absolutePath)) { "Unable to create PTY" }
 private var closed = false
 private val scope = CoroutineScope(SupervisorJob()+Dispatchers.IO)
 init {
  Pty.resize(handle[0],24,80)
  scope.launch {
   val decoder=Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE)
   val pending=ByteBuffer.allocate(16384); val chars=CharBuffer.allocate(16384)
   while(isActive) {
    val bytes=Pty.read(handle[0]) ?: break
    pending.put(bytes).flip(); decoder.decode(pending,chars,false); pending.compact();chars.flip()
    val text=chars.toString();chars.clear()
    withContext(Dispatchers.Main){ output=(output+text).takeLast(2_000_000) }
   }
   withContext(Dispatchers.Main){running=false}
  }
 }
 fun send(text: String) { scope.launch { Pty.write(handle[0],text.toByteArray()) } }
 fun close(){if(!closed){closed=true;Pty.close(handle[0],handle[1])};running=false;scope.cancel()}
}
object Sessions {
 val items=mutableStateListOf<Session>()
 private var next=1
 fun create(directory:File):Session=Session(next++,directory).also{items.add(it)}
 fun close(session:Session){session.close();items.remove(session)}
 fun closeAll(){items.toList().forEach{close(it)}}

}
