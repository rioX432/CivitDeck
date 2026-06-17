plugins {
    id("civitdeck.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:core-domain"))
            implementation(project(":core:core-network"))
            implementation(project(":core:core-database"))
            implementation(libs.koin.core)
            implementation(libs.kotlinx.serialization.json)
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

        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.riox432.civitdeck.core.data"
}
