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

    // Two builds (owner's rule R3, HOUSE_RULES.md; the stage build was dropped by the owner 2026-10-15 ≈ 15:55 +03:00:
    // «не нужно три варианта сборки. Два достаточно. Для разработки дебаг. Прод версия.»): debug is «Debug KARTUN»,
    // prod is just «KARTUN»; the .debug suffix lets both install side by side on the Titan.
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Debug KARTUN")
        }
        release {
            resValue("string", "app_name", "KARTUN")
        }
    }
    // The «Отладка» section and the engine probe screen live in src/debug (code and strings ship only in «Debug KARTUN»,
    // the owner, chat 2026-10-15 ≈ 15:35 +03:00: «в дебаг сборке приложения»); release gets the empty DebugTools of
    // src/nodebug with the same API.
    sourceSets {
        getByName("release") { kotlin.directories.add("src/nodebug/kotlin") }
    }
    // Model files stay uncompressed in the APK: ModelStore reads their size through AssetManager.openFd, which only
    // works for stored (not deflated) assets, and the copy out of the APK is a plain stream.
    androidResources {
        noCompress += listOf("param", "bin")
    }
    buildFeatures {
        compose = true
        resValues = true
        // BuildConfig.DEBUG gates the adb engine probe out of release builds (plans/03 step 12).
        buildConfig = true
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
