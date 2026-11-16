package dev.nexterm

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.*
import java.io.File

class TerminalService : Service() {
 private val scope by lazy { CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate) }
 override fun onCreate() {
  super.onCreate()
  // The visible activity prepares cold notification classes before Android starts
  // the foreground-service deadline. Register before any PTY or UI work.
  startForeground(1, preparedNotification ?: prepare(this))
 }
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
  if(intent?.action==ACTION_NEW_SESSION) scope.launch {
   try {
    val directory=requireNotNull(intent.getStringExtra(EXTRA_DIRECTORY))
    val session=Sessions.create(File(directory))
    intent.getStringExtra(EXTRA_COMMAND)?.let { session.send(it+"\r") }
   }catch(e:CancellationException){throw e}
   catch(e:Exception){Sessions.error=e.message ?: "Unable to start shell"}
   finally {
    Sessions.starting=false
    if(Sessions.items.isEmpty()) stopSelf()
   }
  }
  return START_NOT_STICKY
 }
 override fun onBind(intent: Intent?): IBinder? = null
 override fun onDestroy() { scope.cancel();Sessions.starting=false;Sessions.closeAll();super.onDestroy() }
 companion object {
  const val ACTION_NEW_SESSION="dev.nexterm.NEW_SESSION"
  const val EXTRA_DIRECTORY="directory"
  const val EXTRA_COMMAND="command"
  private var preparedNotification:Notification?=null
  fun prepare(context:Context):Notification {
   preparedNotification?.let { return it }
   val app=context.applicationContext
   app.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("terminal", "Terminal sessions", NotificationManager.IMPORTANCE_LOW))
   val open=PendingIntent.getActivity(app,0,Intent(app,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
   return Notification.Builder(app,"terminal").setContentTitle("NexTerm terminal active")
    .setContentText("Tap to return to your sessions").setSmallIcon(android.R.drawable.ic_dialog_info)
    .setContentIntent(open).setOngoing(true).build().also { preparedNotification=it }
  }
 }
}
