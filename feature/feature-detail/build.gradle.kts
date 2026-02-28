plugins {
    id("civitdeck.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(project(":feature:feature-gallery"))
            implementation(project(":feature:feature-collections"))
        }
    }
}

android {
    namespace = "com.riox432.civitdeck.feature.detail"
}
