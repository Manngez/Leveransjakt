plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "se.leveransjakt.app"
 compileSdk = 35
 defaultConfig { applicationId = "se.leveransjakt.app"; minSdk = 26; targetSdk = 35; versionCode = 5; versionName = "0.5" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies { implementation("androidx.media:media:1.7.0") }
