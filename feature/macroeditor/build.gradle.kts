plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android); alias(libs.plugins.kotlin.compose); alias(libs.plugins.kotlin.serialization) }
android { namespace = "com.trigger.feature.macroeditor"; compileSdk = 35; defaultConfig { minSdk = 26 }; buildFeatures { compose = true } }
dependencies {
 implementation(platform(libs.androidx.compose.bom)); implementation(libs.compose.ui); implementation(libs.compose.foundation); implementation(libs.compose.material3)
 implementation(libs.lifecycle.viewmodel.compose); implementation(libs.kotlinx.serialization.json); implementation(libs.core.ktx)
}
