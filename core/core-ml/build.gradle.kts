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

        // SigLipTokenizer is androidMain-only, so its parity test runs as an
        // Android host (JVM) unit test — task :core:core-ml:testAndroidHostTest.
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.test)
        }
    }

    android {
        namespace = "com.riox432.civitdeck.core.ml"
    }
}
