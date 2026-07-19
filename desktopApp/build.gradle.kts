import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":shared"))
            implementation(project(":core:core-ui"))
            implementation(project(":feature:feature-search"))
            implementation(project(":feature:feature-detail"))
            implementation(project(":feature:feature-gallery"))
            implementation(project(":feature:feature-creator"))
            implementation(project(":feature:feature-collections"))
            implementation(project(":feature:feature-prompts"))
            implementation(project(":feature:feature-settings"))
            implementation(project(":feature:feature-comfyui"))
            implementation(project(":feature:feature-externalserver"))
            implementation(compose.desktop.currentOs)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.okhttp)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.zxing.core)
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(compose.desktop.uiTestJUnit4)
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.riox432.civitdeck.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "CivitDeck"
            packageVersion = "2.4.0"
            description = "Browse and manage CivitAI models from your desktop"
            vendor = "riox432"

            macOS {
                bundleID = "com.riox432.civitdeck"
                minimumSystemVersion = "12.0"
            }
        }
    }
}
