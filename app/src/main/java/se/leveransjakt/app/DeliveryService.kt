package se.leveransjakt.app

import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.*
import android.view.KeyEvent
import java.text.SimpleDateFormat
import java.util.*

class DeliveryService : Service() {
 private var session: MediaSession? = null
 private val clock = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
 override fun onBind(intent: Intent?) = null
 override fun onCreate() {
  super.onCreate()
  val channel = NotificationChannel("delivery", "Leveransläge", NotificationManager.IMPORTANCE_LOW)
  getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
  val notification = Notification.Builder(this, "delivery").setSmallIcon(android.R.drawable.ic_menu_mylocation)
   .setContentTitle("Leveransjakt – leveransläge").setContentText("Testar hörlurarnas nästa-låt-knapp")
   .setContentIntent(open).setOngoing(true).build()
  startForeground(101, notification)
  val s = MediaSession(this, "LeveransjaktLeveranslage")
  s.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS)
  s.setCallback(object : MediaSession.Callback() {
   override fun onSkipToNext() { record("MEDIA_NEXT", "Hörlur/mediekommando"); gps() }
   override fun onMediaButtonEvent(intent: Intent): Boolean {
    @Suppress("DEPRECATION")
    val event = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
      else intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
    if (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
     onSkipToNext(); return true
    }
    return super.onMediaButtonEvent(intent)
   }
  })
  s.setPlaybackState(PlaybackState.Builder().setActions(PlaybackState.ACTION_SKIP_TO_NEXT)
    .setState(PlaybackState.STATE_PLAYING, 0, 1f).build())
  session = s
  s.isActive = true
  record("LEVERANSLAGE_START", "Mediesession aktiv")
 }
 private fun record(kind: String, detail: String) {
  val prefs = getSharedPreferences("leveransjakt", MODE_PRIVATE)
  val entry = "${clock.format(Date())};$kind;${detail.replace(';', ',')}"
  val previous = prefs.getString("events", "").orEmpty()
  prefs.edit().putString("events", if (previous.isBlank()) entry else "$previous\n$entry").apply()
  sendBroadcast(Intent("se.leveransjakt.app.LOG_UPDATED").setPackage(packageName))
 }
 private fun gps() {
  if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
      checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
   record("GPS_FEL", "Platsbehörighet saknas"); return
  }
  val manager = getSystemService(LOCATION_SERVICE) as LocationManager
  val provider = when {
   manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
   manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
   else -> null
  } ?: run { record("GPS_FEL", "Plats avstängd"); return }
  manager.getCurrentLocation(provider, null, mainExecutor) { loc ->
   if (loc != null) record("LEVERANSSTOPP", "${loc.latitude},${loc.longitude}; noggrannhet ${loc.accuracy}m")
   else record("GPS_FEL", "Ingen position")
  }
 }
 override fun onDestroy() {
  session?.isActive = false
  session?.release()
  session = null
  super.onDestroy()
 }
}
