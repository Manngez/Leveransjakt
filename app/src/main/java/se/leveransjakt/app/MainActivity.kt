package se.leveransjakt.app

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
 private lateinit var output: TextView
 private val events = mutableListOf<String>()
 private val clock = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 30, 24, 20) }
  val heading = TextView(this).apply { text = "Leveransjakt"; textSize = 28f }
  root.addView(heading)
  root.addView(TextView(this).apply { text = "Säker testversion: registrera GPS-stopp manuellt. Automatisk Spotify- och hörlursavkänning är avstängd." })
  fun action(label: String, fn: () -> Unit) {
   root.addView(Button(this).apply { text = label; setOnClickListener { fn() } })
  }
  action("Tillåt GPS") {
   requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
  }
  action("Registrera leveransstopp") { saveStop() }
  action("Markera hörlurstryck manuellt") { add("TESTMARKERING", "Manuell markering, ingen automatisk avkänning") }
  action("Dela logg") {
   val csv = (listOf("tid;typ;detalj") + events).joinToString("\n")
   val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, csv) }
   startActivity(Intent.createChooser(intent, "Dela logg"))
  }
  output = TextView(this).apply { textSize = 13f }
  root.addView(ScrollView(this).apply { addView(output) }, LinearLayout.LayoutParams(-1, 0, 1f))
  setContentView(root)
  events.addAll(getSharedPreferences("leveransjakt", MODE_PRIVATE).getString("events", "")!!.lines().filter { it.isNotBlank() })
  render()
 }
 private fun saveStop() {
  if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
      checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
   Toast.makeText(this, "Tillåt GPS först", Toast.LENGTH_SHORT).show(); return
  }
  val manager = getSystemService(LOCATION_SERVICE) as LocationManager
  val provider = when {
   manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
   manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
   else -> null
  }
  if (provider == null) { Toast.makeText(this, "Aktivera plats", Toast.LENGTH_SHORT).show(); return }
  if (Build.VERSION.SDK_INT >= 30) {
   manager.getCurrentLocation(provider, null, mainExecutor) { loc ->
    if (loc != null) add("LEVERANSSTOPP", "${loc.latitude},${loc.longitude}; noggrannhet ${loc.accuracy}m")
    else Toast.makeText(this, "Ingen GPS-position", Toast.LENGTH_SHORT).show()
   }
  } else {
   @Suppress("DEPRECATION") val loc = manager.getLastKnownLocation(provider)
   if (loc != null) add("LEVERANSSTOPP", "${loc.latitude},${loc.longitude}; äldre position")
  }
 }
 private fun add(kind: String, detail: String) {
  events.add("${clock.format(Date())};$kind;${detail.replace(';', ',')}")
  getSharedPreferences("leveransjakt", MODE_PRIVATE).edit().putString("events", events.joinToString("\n")).apply()
  render()
 }
 private fun render() { output.text = events.takeLast(80).asReversed().joinToString("\n\n").ifEmpty { "Inga händelser ännu" } }
}
