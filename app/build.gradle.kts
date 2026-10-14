// :app - the Compose UI: the main path of phase 2 (plans/03) plus the phase 0 engine probe driven by adb intent extras.
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

    // App names on the phone (owner's rule R3, HOUSE_RULES.md): stage builds carry "Stage KARTUN", prod is just "KARTUN".
    // Each type has its own applicationId suffix, so debug, stage and prod install side by side on the Titan.
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Debug KARTUN")
        }
        create("stage") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".stage"
            resValue("string", "app_name", "Stage KARTUN")
            matchingFallbacks += listOf("debug")
        }
        release {
            resValue("string", "app_name", "KARTUN")
        }
    }
    // Model files stay uncompressed in the APK: ModelStore reads their size through AssetManager.openFd, which only
    // works for stored (not deflated) assets, and the copy out of the APK is a plain stream.
    androidResources {
        noCompress += listOf("param", "bin")
    }
    buildFeatures {
        compose = true
        resValues = true
    }
}

dependencies {
    implementation(project(":engine"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
}
