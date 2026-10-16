// :engine - the native upscaler: C++ over our ncnn fork MikalaiKryvusha/ncnn (tools/build-ncnn.mjs), exposed to Kotlin through JNI.
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.kriniks.kartun.engine"
    compileSdk = 37
    ndkVersion = "29.0.14206865"

    defaultConfig {
        // R8 keep rules for the names the C++ side calls back (TileListener.onTile) - consumer-rules.pro.
        consumerProguardFiles("consumer-rules.pro")
        minSdk = 34
        ndk { abiFilters += "arm64-v8a" }
        externalNativeBuild {
            cmake { arguments += listOf("-DANDROID_STL=c++_static") }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.31.6"
        }
    }
}
