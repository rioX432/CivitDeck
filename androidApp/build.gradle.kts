plugins {
    id("civitdeck.android.application")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.aboutlibraries)
}

// Gate embedding-based similarity search. Default off until SigLIP-2 embeddings are
// being produced on-device (parent #602, phases C/D). While off, the 52 MB SigLIP-2
// asset and the ONNX runtime native libraries (~112 MB across ABIs) are excluded from
// the APK; ImageEmbeddingModelImpl detects the missing asset and reports unavailable,
// so all embedding call sites no-op. Enable with -Pcivitdeck.enableSimilaritySearch=true.
val similaritySearchEnabled =
    providers.gradleProperty("civitdeck.enableSimilaritySearch").orNull == "true"

android {
    namespace = "com.riox432.civitdeck"

    defaultConfig {
        applicationId = "com.riox432.civitdeck"
        versionCode = 8
        versionName = "2.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("boolean", "FEATURE_SIMILARITY_SEARCH", similaritySearchEnabled.toString())
    }

    sourceSets {
        named("main") {
            if (similaritySearchEnabled) {
                assets.srcDir("src/similarity/assets")
            }
        }
    }

    // Release signing config from environment variables (CI)
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("RELEASE_KEYSTORE")
            if (keystorePath != null && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        if (!similaritySearchEnabled) {
            jniLibs {
                excludes += "**/libonnxruntime*.so"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val releaseKeystore = System.getenv("RELEASE_KEYSTORE")
            if (releaseKeystore != null && file(releaseKeystore).exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)

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
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.paging.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.material3.adaptive)
    implementation(libs.material3.adaptive.navigation.suite)
    implementation(libs.aboutlibraries.compose.m3)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.zxing.core)
    implementation(libs.mlkit.barcode)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
}
