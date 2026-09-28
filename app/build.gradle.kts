plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
}

val releaseCode=(providers.gradleProperty("releaseVersionCode").orNull ?: "1").toInt()
val releaseName=providers.gradleProperty("releaseVersionName").orNull ?: "1.0.0"
val signingStore=providers.gradleProperty("TRIGGER_RELEASE_STORE_FILE").orNull ?: System.getenv("TRIGGER_RELEASE_STORE_FILE")
val signingPassword=providers.gradleProperty("TRIGGER_RELEASE_STORE_PASSWORD").orNull ?: System.getenv("TRIGGER_RELEASE_STORE_PASSWORD")
val signingAlias=providers.gradleProperty("TRIGGER_RELEASE_KEY_ALIAS").orNull ?: System.getenv("TRIGGER_RELEASE_KEY_ALIAS")
val signingKeyPassword=providers.gradleProperty("TRIGGER_RELEASE_KEY_PASSWORD").orNull ?: System.getenv("TRIGGER_RELEASE_KEY_PASSWORD")
val releaseSigningConfigured=listOf(signingStore,signingPassword,signingAlias,signingKeyPassword).all { !it.isNullOrBlank() }

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    namespace = "com.trigger.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.trigger.app"
        minSdk = 26
        targetSdk = 35
        versionCode = releaseCode
        versionName = releaseName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    signingConfigs {
        if(releaseSigningConfigured) create("release") {
            storeFile=file(signingStore!!); storePassword=signingPassword; keyAlias=signingAlias; keyPassword=signingKeyPassword
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if(releaseSigningConfigured) signingConfig=signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro")
            buildConfigField("boolean","RELEASE_SIGNING_CONFIGURED",releaseSigningConfigured.toString())
            buildConfigField("String","RELEASE_VERSION_NAME","\"$releaseName\"")
        }
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
baselineProfile { saveInSrc = true }

dependencies {
    implementation(project(":core:common")); implementation(project(":core:ui")); implementation(project(":core:database"))
    implementation(project(":automation:engine")); baselineProfile(project(":baselineprofile")); implementation(project(":feature:gameassistant")); implementation(project(":feature:macroeditor")); implementation(project(":feature:scheduling")); implementation(project(":feature:settings"))
    implementation(platform(libs.androidx.compose.bom)); implementation(libs.compose.ui); implementation(libs.compose.foundation); implementation(libs.compose.material3); implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose); implementation(libs.core.ktx); implementation(libs.lifecycle.runtime.ktx); implementation(libs.lifecycle.runtime.compose); implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.shizuku.api); implementation(libs.shizuku.provider); implementation(libs.koin.android); implementation(libs.koin.compose); implementation(libs.navigation.compose); implementation(libs.work.runtime.ktx)
    androidTestImplementation(libs.androidx.test.core); androidTestImplementation(libs.androidx.test.runner); androidTestImplementation(libs.androidx.test.ext.junit)
}


kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "21"
}
