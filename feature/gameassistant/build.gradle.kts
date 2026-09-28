plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android); alias(libs.plugins.kotlin.compose) }
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    } namespace = "com.trigger.feature.gameassistant"; compileSdk = 35; defaultConfig { minSdk = 26 } ; buildFeatures { compose = true } }
dependencies {
    implementation(platform(libs.androidx.compose.bom)); implementation(libs.compose.ui); implementation(libs.compose.foundation); implementation(libs.compose.material3)
    implementation(libs.activity.compose); implementation(libs.core.ktx); implementation(libs.lifecycle.service)
    testImplementation(libs.junit); testImplementation(libs.robolectric)
}
android { testOptions { unitTests.isIncludeAndroidResources = true } }


kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "21"
}
