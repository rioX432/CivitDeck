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
    }
}

compose.desktop {
    application {
        mainClass = "com.riox432.civitdeck.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "CivitDeck"
            packageVersion = "2.2.0"
            description = "Browse and manage CivitAI models from your desktop"
            vendor = "riox432"

            macOS {
                bundleID = "com.riox432.civitdeck"
                minimumSystemVersion = "12.0"
            }
        }
    }
}
