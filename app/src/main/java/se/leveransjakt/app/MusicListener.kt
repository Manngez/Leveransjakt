package se.leveransjakt.app

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent

class MusicListener : NotificationListenerService() {
 override fun onNotificationPosted(sbn: StatusBarNotification?) {
  if (sbn?.packageName != "com.spotify.music") return
  val n = sbn.notification ?: return
  val title = n.extras.getCharSequence("android.title")?.toString() ?: return
  val artist = n.extras.getCharSequence("android.text")?.toString() ?: ""
  val track = "$title — $artist"
  val prefs = getSharedPreferences("leveransjakt", MODE_PRIVATE)
  if (prefs.getString("last_track", "") == track) return
  prefs.edit().putString("last_track", track).apply()
  sendBroadcast(Intent(ACTION_TRACK).setPackage(packageName).putExtra("track", track))
 }
 companion object { const val ACTION_TRACK = "se.leveransjakt.app.TRACK_CHANGED" }
}