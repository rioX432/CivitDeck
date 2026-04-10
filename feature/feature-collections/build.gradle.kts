plugins {
    id("civitdeck.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(project(":core:core-plugin"))
        }
    }
}

android {
    namespace = "com.riox432.civitdeck.feature.collections"
}
