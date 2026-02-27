plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        create("kmpLibrary") {
            id = "civitdeck.kmp.library"
            implementationClass = "CivitDeckKmpLibraryPlugin"
        }
        create("androidApplication") {
            id = "civitdeck.android.application"
            implementationClass = "CivitDeckAndroidApplicationPlugin"
        }
    }
}
