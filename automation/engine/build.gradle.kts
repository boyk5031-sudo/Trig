plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android) }
android { namespace = "com.trigger.automation.engine"; compileSdk = 35; defaultConfig { minSdk = 26 } }
dependencies { implementation(project(":core:common")); implementation(libs.shizuku.api) }
