package se.leveransjakt.app

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import java.text.SimpleDateFormat
import java.util.*

class DeliveryService : Service(), LocationListener {
 private lateinit var manager: LocationManager
 private val clock = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
 override fun onBind(intent: Intent?): IBinder? = null

 override fun onCreate() {
  super.onCreate()
  val channel = NotificationChannel("route", "GPS-rutt pågår", NotificationManager.IMPORTANCE_LOW)
  getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val notification = Notification.Builder(this, "route")
   .setSmallIcon(android.R.drawable.ic_menu_mylocation)
   .setContentTitle("Leveransjakt – GPS-rutt pågår")
   .setContentText("Tryck för att öppna och stoppa rutten")
   .setContentIntent(open).setOngoing(true).build()
  if (Build.VERSION.SDK_INT >= 29) startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
  else startForeground(101, notification)
  manager = getSystemService(LOCATION_SERVICE) as LocationManager
  val prefs = getSharedPreferences("leveransjakt", MODE_PRIVATE)
  prefs.edit().putBoolean("tracking", true).apply()
  log("RUTT_START", "")
  try {
   if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
       manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
    manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15000L, 15f, this, Looper.getMainLooper())
   } else if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
              manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
    manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 15000L, 15f, this, Looper.getMainLooper())
   } else log("GPS_FEL", "Ingen aktiv platsleverantör eller behörighet")
  } catch (e: SecurityException) { log("GPS_FEL", "Platsbehörighet saknas") }
 }
 override fun onLocationChanged(location: Location) {
  val detail = "${location.latitude},${location.longitude},${location.accuracy},${location.provider}"
  log("GPS", detail)
 }
 private fun log(kind: String, detail: String) {
  val prefs = getSharedPreferences("leveransjakt", MODE_PRIVATE)
  val entry = "${clock.format(Date())};$kind;$detail"
  val previous = prefs.getString("events", "").orEmpty()
  prefs.edit().putString("events", if (previous.isBlank()) entry else "$previous\n$entry").apply()
 }
 override fun onDestroy() {
  if (::manager.isInitialized) manager.removeUpdates(this)
  log("RUTT_STOPP", "")
  getSharedPreferences("leveransjakt", MODE_PRIVATE).edit().putBoolean("tracking", false).apply()
  super.onDestroy()
 }
}
