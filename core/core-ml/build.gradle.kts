plugins {
    id("civitdeck.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:core-domain"))
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
        }

        androidMain.dependencies {
            implementation(libs.onnxruntime.android)
            implementation(libs.kotlinx.serialization.json)
        }
    }

    android {
        namespace = "com.riox432.civitdeck.core.ml"
    }
}
