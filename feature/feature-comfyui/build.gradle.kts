plugins {
    id("civitdeck.kmp.feature")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.ktor.client.core)
        }
    }
}

android {
    namespace = "com.riox432.civitdeck.feature.comfyui"
}
