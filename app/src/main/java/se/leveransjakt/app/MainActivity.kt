package se.leveransjakt.app

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.view.KeyEvent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
 private lateinit var output: TextView
 private var mediaTest: MediaSession? = null
 private val events = mutableListOf<String>()
 private val clock = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 30, 24, 20) }
  val heading = TextView(this).apply { text = "Leveransjakt"; textSize = 28f }
  root.addView(heading)
  root.addView(TextView(this).apply { text = "Version 0.3: experiment med medieknappar. Android kan skicka dubbeltryck till Spotify i stället. Testet kan påverka Spotifys knappar och fungerar bara när appen är öppen." })
  fun action(label: String, fn: () -> Unit) {
   root.addView(Button(this).apply { text = label; setOnClickListener { fn() } })
  }
  action("Tillåt GPS") {
   requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
  }
  action("Registrera leveransstopp") { saveStop() }
  action("Starta medieknappstest") { startMediaTest() }
  action("Stoppa medieknappstest") { stopMediaTest() }
  action("Markera hörlurstryck manuellt") { add("TESTMARKERING", "Manuell markering") }
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
 override fun onStop() { stopMediaTest(); super.onStop() }
 override fun onDestroy() { mediaTest?.release(); mediaTest = null; super.onDestroy() }
 private fun startMediaTest() {
  if (mediaTest != null) return
  val session = MediaSession(this, "LeveransjaktKnappTest")
  session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS)
  session.setCallback(object : MediaSession.Callback() {
   override fun onSkipToNext() { add("MEDIA_NEXT", "Mediekommando mottaget"); saveStop() }
   override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
    @Suppress("DEPRECATION")
    val event = if (Build.VERSION.SDK_INT >= 33)
     mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
    else mediaButtonIntent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
    if (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
     onSkipToNext()
     return true
    }
    return super.onMediaButtonEvent(mediaButtonIntent)
   }
  })
  session.setPlaybackState(PlaybackState.Builder()
   .setActions(PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_PLAY_PAUSE)
   .setState(PlaybackState.STATE_PLAYING, 0L, 1f).build())
  mediaTest = session
  session.isActive = true
  add("TEST_START", "Mediesession aktiv; Spotify kan påverkas")
 }
 private fun stopMediaTest() {
  mediaTest?.isActive = false
  mediaTest?.release()
  mediaTest = null
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
