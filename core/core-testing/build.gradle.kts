plugins {
    id("civitdeck.kmp.library")
}

// Test-support module: shared fakes, fixtures and coroutine test helpers.
// Lives in commonMain so feature/core test source sets can depend on it
// (Gradle does not allow depending on another module's commonTest).
// IMPORTANT: no production module may depend on this module — test only.
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:core-domain"))
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.test)
        }
    }

    android {
        namespace = "com.riox432.civitdeck.core.testing"
    }
}
