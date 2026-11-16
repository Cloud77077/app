package dev.nexterm
import android.app.*
import android.content.Intent
import android.os.IBinder
class TerminalService : Service() {
 override fun onCreate() { super.onCreate()
  getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("terminal", "Terminal sessions", NotificationManager.IMPORTANCE_LOW))
  val open = PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
  startForeground(1, Notification.Builder(this,"terminal").setContentTitle("NexTerm terminal active").setContentText("Tap to return to your sessions").setSmallIcon(android.R.drawable.ic_dialog_info).setContentIntent(open).setOngoing(true).build())
 }
 override fun onBind(intent: Intent?): IBinder? = null
 override fun onDestroy() { Sessions.closeAll(); super.onDestroy() }
}
