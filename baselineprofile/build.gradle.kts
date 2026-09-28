plugins { alias(libs.plugins.android.test); alias(libs.plugins.baselineprofile) }
android {
    namespace = "com.trigger.baselineprofile"
    compileSdk = 35
    defaultConfig { minSdk = 28; targetSdk = 35; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    targetProjectPath = ":app"
}
dependencies { implementation(libs.benchmark.macro.junit4); implementation(libs.androidx.test.uiautomator) }
