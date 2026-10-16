// :probe - a test-only caller for criterion A7 of the agent API (plans/07 step 9): an app of ANOTHER uid, without the DUMP
// permission, that CAN see KARTUN (<queries>), so the door's DUMP guard - not package visibility - is what answers it.
// Installed for the run and removed after it; never shipped (it is not a dependency of :app).
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.kriniks.kartun.probe"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kriniks.kartun.probe"
        minSdk = 36
        targetSdk = 37
        versionCode = 1
        versionName = "1"
    }
}
