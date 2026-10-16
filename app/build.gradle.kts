// :app - the Compose UI: the main path of phase 2 (plans/03) plus the phase 0 engine probe driven by adb intent extras.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    // The engine's NDK (engine/build.gradle.kts): AGP strips the native libraries with it - without it the default NDK is
    // looked up, is not installed here, and libkartun_engine.so shipped with its debug symbols («Unable to strip»).
    ndkVersion = "29.0.14206865"
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

    // The release key lives in .env + kartun-release.jks at the project root, outside git (owner's rule R2; made by
    // tools/make-release-key.py on the owner's word 2026-10-16 «ты создай в .env, вне гита»). Without .env the release
    // build stays unsigned - a clean clone still builds. [NOT-TESTED]
    val releaseEnv = rootProject.file(".env").takeIf { it.exists() }?.readLines()
        ?.mapNotNull { line -> line.trim().takeIf { it.isNotEmpty() && !it.startsWith("#") && "=" in it } }
        ?.associate { it.substringBefore("=").trim() to it.substringAfter("=").trim() }
    if (releaseEnv != null) signingConfigs {
        create("release") {
            storeFile = rootProject.file(releaseEnv.getValue("KARTUN_KEYSTORE"))
            storePassword = releaseEnv.getValue("KARTUN_KEYSTORE_PASSWORD")
            keyAlias = releaseEnv.getValue("KARTUN_KEY_ALIAS")
            keyPassword = releaseEnv.getValue("KARTUN_KEY_PASSWORD")
        }
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
            // R8 for the prod build (the owner, MASTER_PLAN 2026-10-15 ≈ 13:57: «нужно будет включить для прод сборок»):
            // code shrinking and obfuscation plus resource shrinking; the engine's JNI keep rules come with :engine.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (releaseEnv != null) signingConfig = signingConfigs.getByName("release")
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
    testImplementation(libs.org.json)
}
