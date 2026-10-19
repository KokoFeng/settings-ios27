plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.ios27.settings"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.ios27.settings"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        buildConfig = false
        aidl = false
        renderScript = false
        shaders = false
        resValues = false
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
