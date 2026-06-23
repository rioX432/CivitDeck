plugins {
    id("civitdeck.kmp.feature")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:core-plugin"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.ktor.client.core)
        }

        commonTest.dependencies {
            implementation(project(":core:core-testing"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }

        // ComfyUISettingsViewModel depends on GenerationNotificationService, whose
        // Android `actual` requires a Context. The JVM `actual` is parameterless, so
        // this ViewModel test lives in jvmTest and runs via the :jvmTest task in CI.
        jvmTest.dependencies {
            implementation(project(":core:core-testing"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.ktor.client.mock)
        }
    }

    android {
        namespace = "com.riox432.civitdeck.feature.comfyui"
    }
}
