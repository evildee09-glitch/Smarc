plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("com.google.devtools.ksp") }
android {
    namespace = "ph.smarc"; compileSdk = 34
    defaultConfig { applicationId = "ph.smarc"; minSdk = 26; targetSdk = 34; versionCode = 2; versionName = "1.1.0"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room"); implementation("androidx.room:room-ktx:$room"); ksp("androidx.room:room-compiler:$room")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.material3:material3"); implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui"); implementation("androidx.activity:activity-compose:1.9.0"); implementation("androidx.core:core-ktx:1.13.1"); implementation("androidx.biometric:biometric:1.1.0"); implementation("androidx.fragment:fragment-ktx:1.8.1"); implementation("net.zetetic:sqlcipher-android:4.5.6@aar"); implementation("androidx.sqlite:sqlite:2.4.0"); implementation("com.google.code.gson:gson:2.10.1"); implementation("androidx.work:work-runtime-ktx:2.9.0"); implementation("androidx.core:core-splashscreen:1.0.1"); implementation("androidx.biometric:biometric:1.1.0"); implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation("net.zetetic:sqlcipher-android:4.5.6@aar"); implementation("androidx.sqlite:sqlite-ktx:2.4.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5"); androidTestImplementation("androidx.test:core-ktx:1.5.0"); androidTestImplementation("androidx.test:runner:1.5.2"); testImplementation("org.json:json:20231013")
}
