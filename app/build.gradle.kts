plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
 namespace = "dev.nexterm"
 compileSdk = 35
 defaultConfig {
  applicationId = "dev.nexterm"; minSdk = 26; targetSdk = 35
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  versionCode = providers.environmentVariable("VERSION_CODE").orElse("1").get().toInt()
  versionName = providers.environmentVariable("VERSION_NAME").orElse("0.2.0-dev").get()
  externalNativeBuild { cmake { cppFlags += "-std=c++17" } }
 }
 signingConfigs { create("release") {
  System.getenv("KEYSTORE_PATH")?.let { storeFile = file(it); storePassword = System.getenv("KEYSTORE_PASSWORD"); keyAlias = System.getenv("KEY_ALIAS"); keyPassword = System.getenv("KEY_PASSWORD") }
 } }
 buildTypes { getByName("release") { if (System.getenv("KEYSTORE_PATH") != null) signingConfig = signingConfigs.getByName("release") } }
 buildFeatures { compose = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }
 ndkVersion = "27.0.12077973"
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2024.12.01"))
 implementation("androidx.activity:activity-compose:1.9.3")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation("androidx.test:runner:1.6.2")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
}
