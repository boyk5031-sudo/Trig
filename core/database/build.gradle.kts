plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android); alias(libs.plugins.ksp) }
android {
    namespace = "com.trigger.core.database"
    compileSdk = 35
    defaultConfig { minSdk = 26; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    implementation(libs.room.runtime); implementation(libs.room.ktx); ksp(libs.room.compiler)
    androidTestImplementation(libs.androidx.test.core); androidTestImplementation(libs.androidx.test.runner); androidTestImplementation(libs.androidx.test.ext.junit); androidTestImplementation(libs.sqlite.framework)
}
