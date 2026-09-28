plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android) }

android {
    namespace = "com.trigger.automation.engine"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    buildFeatures { aidl = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":feature:macroeditor"))
    implementation(libs.shizuku.api)
    implementation(libs.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
}
