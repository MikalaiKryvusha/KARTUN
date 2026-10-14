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
