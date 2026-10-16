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
    }
}
rootProject.name = "KARTUN"
include(":app", ":engine")
// :probe - the test-only A7 caller of the agent door (plans/07 step 9); not part of KARTUN's APK.
include(":probe")
