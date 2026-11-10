package se.leveransjakt.app

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*

class MainActivity : Activity() {
 private lateinit var output: TextView
 private lateinit var status: TextView
 private val prefs by lazy { getSharedPreferences("leveransjakt", MODE_PRIVATE) }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 32, 28, 20) }
  root.addView(TextView(this).apply { text = "Leveransjakt"; textSize = 28f })
  root.addView(TextView(this).apply { text = "GPS-logg 1.0 – registrerar position ungefär var 15:e sekund vid förflyttning minst 15 meter. Starta och stoppa själv." })
  status = TextView(this).apply { textSize = 18f }
  root.addView(status)
  fun action(label: String, callback: () -> Unit) {
   root.addView(Button(this).apply { text = label; setOnClickListener { callback() } })
  }
  action("1. Tillåt plats (exakt position)") {
   requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
  }
  action("2. Starta rutt") {
   if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
       checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
    Toast.makeText(this, "Ge platsbehörighet först", Toast.LENGTH_LONG).show()
   } else {
    startForegroundService(Intent(this, DeliveryService::class.java))
    status.text = "Rutt startad"
   }
  }
  action("Stoppa rutt") { stopService(Intent(this, DeliveryService::class.java)); refresh() }
  action("Uppdatera logg") { refresh() }
  action("Exportera CSV") {
   val csv = "tid;typ;latitud,longitud,noggrannhet_meter,leverantör\n" + prefs.getString("events", "").orEmpty()
   val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, csv) }
   startActivity(Intent.createChooser(send, "Dela GPS-logg"))
  }
  action("Rensa logg (endast när rutten är stoppad)") {
   if (prefs.getBoolean("tracking", false)) Toast.makeText(this, "Stoppa rutten först", Toast.LENGTH_SHORT).show()
   else { prefs.edit().remove("events").apply(); refresh() }
  }
  output = TextView(this).apply { textSize = 13f; setTextIsSelectable(true) }
  root.addView(ScrollView(this).apply { addView(output) }, LinearLayout.LayoutParams(-1, 0, 1f))
  setContentView(root)
  refresh()
 }
 override fun onResume() { super.onResume(); if (::output.isInitialized) refresh() }
 private fun refresh() {
  status.text = if (prefs.getBoolean("tracking", false)) "● GPS-rutt pågår" else "○ Ingen rutt pågår"
  val lines = prefs.getString("events", "").orEmpty().lines().filter { it.isNotBlank() }
  val points = lines.count { it.contains(";GPS;") }
  output.text = "Registrerade GPS-punkter: $points\n\n" + lines.takeLast(100).asReversed().joinToString("\n")
 }
}
