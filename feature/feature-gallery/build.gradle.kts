plugins {
    id("civitdeck.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.serialization.json)
            implementation(project(":feature:feature-prompts"))
        }
    }
}

android {
    namespace = "com.riox432.civitdeck.feature.gallery"
}
