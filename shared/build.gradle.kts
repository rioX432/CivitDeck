plugins {
    id("civitdeck.kmp.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.skie)
}

kotlin {
    listOf(
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
            api(project(":core:core-plugin"))
            implementation(project(":feature:feature-settings"))
            implementation(project(":feature:feature-creator"))
            implementation(project(":feature:feature-prompts"))
            implementation(project(":feature:feature-gallery"))
            implementation(project(":feature:feature-collections"))
            implementation(project(":feature:feature-detail"))
            implementation(project(":feature:feature-comfyui"))
            implementation(project(":feature:feature-search"))
            implementation(project(":feature:feature-externalserver"))
            api(libs.androidx.lifecycle.viewmodel)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
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

skie {
    features {
        enableSwiftUIObservingPreview = true
    }
}

android {
    namespace = "com.riox432.civitdeck.shared"
}
