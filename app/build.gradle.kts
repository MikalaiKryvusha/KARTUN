// :app - the Compose UI. Phase 0 holds only a debug probe screen that drives the engine.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.kriniks.kartun"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kriniks.kartun"
        minSdk = 36
        targetSdk = 37
        versionCode = 1
        versionName = "0.1 (1)"
        ndk { abiFilters += "arm64-v8a" }
    }

    buildTypes {
        debug { applicationIdSuffix = ".debug" }
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":engine"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
}
