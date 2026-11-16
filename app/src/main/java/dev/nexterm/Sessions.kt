package dev.nexterm

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction

class Session(val id: Int, directory: File) {
 var name by mutableStateOf("Shell $id")
 val screen = TerminalScreen()
 var revision by mutableIntStateOf(0); private set
 var running by mutableStateOf(true); private set
 var error by mutableStateOf<String?>(null); private set
 private val handle = requireNotNull(Pty.open(directory.absolutePath)) { "Unable to create PTY" }
 private val lock = Any()
 private var closed = false
 private val scope = CoroutineScope(SupervisorJob()+Dispatchers.IO)
 private val input = Channel<ByteArray>(256)
 init {
  screen.reply = { send(it) }
  Pty.resize(handle[0],screen.rows,screen.columns)
  scope.launch {
   for (bytes in input) {
    var offset = 0
    while(offset < bytes.size && isActive) {
     val n = synchronized(lock) { if(closed) -1 else Pty.write(handle[0],bytes.copyOfRange(offset,bytes.size)) }
     if(n < 0) { withContext(Dispatchers.Main) { error="Shell input closed" }; break }
     offset += n
     if(n == 0) delay(10)
    }
   }
  }
  scope.launch {
   val decoder=Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE)
   val pending=ByteBuffer.allocate(16384); val chars=CharBuffer.allocate(16384)
   try {
    while(isActive) {
     val bytes=synchronized(lock) { if(closed) null else Pty.read(handle[0]) } ?: break
     if(bytes.isEmpty()) { delay(16); continue }
     pending.put(bytes).flip(); decoder.decode(pending,chars,false); pending.compact();chars.flip()
     val text=chars.toString();chars.clear()
     withContext(Dispatchers.Main){ screen.feed(text); revision++ }
    }
    pending.flip(); decoder.decode(pending,chars,true); decoder.flush(chars); chars.flip()
    val tail=chars.toString()
    withContext(Dispatchers.Main){ if(tail.isNotEmpty()) { screen.feed(tail); revision++ };running=false }
   } finally { release() }
  }
 }
 fun send(text: String) { if(running && input.trySend(text.toByteArray()).isFailure) error="Input queue full; try again" }
 fun resize(rows:Int,columns:Int) {
  if(rows==screen.rows && columns==screen.columns) return
  screen.resize(rows,columns); revision++
  synchronized(lock) { if(!closed) Pty.resize(handle[0],screen.rows,screen.columns) }
 }
 private fun release() { synchronized(lock) { if(!closed){closed=true;Pty.close(handle[0],handle[1])} };input.close() }
 fun close(){release();running=false;scope.cancel()}
}
object Sessions {
 val items=mutableStateListOf<Session>()
 private var next=1
 fun create(directory:File):Session=Session(next++,directory).also{items.add(it)}
 fun close(session:Session){session.close();items.remove(session)}
 fun closeAll(){items.toList().forEach{close(it)}}
}
