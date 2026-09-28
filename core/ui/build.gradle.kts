plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android); alias(libs.plugins.kotlin.compose) }
android { namespace = "com.trigger.core.ui"; compileSdk = 35; defaultConfig { minSdk = 26 }; buildFeatures { compose = true } }
dependencies { implementation(platform(libs.androidx.compose.bom)); implementation(libs.compose.ui); implementation(libs.compose.foundation); implementation(libs.compose.material3) }
