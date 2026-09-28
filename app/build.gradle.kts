plugins { alias(libs.plugins.android.application); alias(libs.plugins.kotlin.android); alias(libs.plugins.kotlin.compose) }
android { namespace = "com.trigger.app"; compileSdk = 35
    defaultConfig { applicationId = "com.trigger.app"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0" }
    buildFeatures { compose = true }
}
dependencies {
    implementation(project(":core:common")); implementation(project(":automation:engine")); implementation(project(":feature:gameassistant"))
    implementation(platform(libs.androidx.compose.bom)); implementation(libs.compose.ui); implementation(libs.compose.foundation); implementation(libs.compose.material3); implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose); implementation(libs.core.ktx); implementation(libs.lifecycle.runtime.ktx); implementation(libs.shizuku.api); implementation(libs.shizuku.provider); implementation(libs.koin.android); implementation(libs.koin.compose)
}
