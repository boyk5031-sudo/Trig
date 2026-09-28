plugins { alias(libs.plugins.android.test); alias(libs.plugins.kotlin.android); alias(libs.plugins.baselineprofile) }
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    namespace = "com.trigger.baselineprofile"
    compileSdk = 35
    defaultConfig { minSdk = 28; targetSdk = 35; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    targetProjectPath = ":app"
}
dependencies { implementation(libs.benchmark.macro.junit4); implementation(libs.androidx.test.uiautomator) }

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "21"
}
