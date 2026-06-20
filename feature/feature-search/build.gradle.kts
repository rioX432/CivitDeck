plugins {
    id("civitdeck.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.paging.common)
        }
        commonTest.dependencies {
            implementation(project(":core:core-testing"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }

    android {
        namespace = "com.riox432.civitdeck.feature.search"
    }
}
