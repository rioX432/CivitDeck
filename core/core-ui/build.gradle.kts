plugins {
    id("civitdeck.kmp.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:core-domain"))
            implementation(project(":core:core-plugin"))
            implementation(libs.koin.core)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.foundation)
            implementation(compose.ui)
        }

        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.okhttp)
        }
    }
}

android {
    namespace = "com.riox432.civitdeck.core.ui"

    buildFeatures {
        compose = true
    }
}
