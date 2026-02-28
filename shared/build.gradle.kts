plugins {
    id("civitdeck.kmp.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.skie)
}

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(libs.androidx.lifecycle.viewmodel)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:core-domain"))
            api(project(":core:core-network"))
            api(project(":core:core-database"))
            api(project(":feature:feature-settings"))
            api(project(":feature:feature-creator"))
            api(project(":feature:feature-prompts"))
            api(project(":feature:feature-gallery"))
            api(project(":feature:feature-collections"))
            api(project(":feature:feature-detail"))
            api(project(":feature:feature-comfyui"))
            api(libs.androidx.lifecycle.viewmodel)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
        }

        androidMain.dependencies {
            implementation(libs.koin.android)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

android {
    namespace = "com.riox432.civitdeck.shared"
}
