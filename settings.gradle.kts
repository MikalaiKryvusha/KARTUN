// KARTUN - Gradle settings. Modules: :app (Compose UI) and :engine (native NCNN upscaler behind JNI).
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack for FreeDroidWarn only (KAST plans/15, the owner 2026-11-07: «во все их нужно встроить FreeDroidWarn»);
        // the content filter keeps every other dependency on Google Maven and Maven Central.
        maven("https://jitpack.io") { content { includeGroup("com.github.woheller69") } }
    }
}
rootProject.name = "KARTUN"
include(":app", ":engine")
// :probe - the test-only A7 caller of the agent door (plans/07 step 9); not part of KARTUN's APK.
include(":probe")
