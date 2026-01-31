plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.detekt)
}

allprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom("$rootDir/config/detekt/detekt.yml")
        autoCorrect = true
    }

    dependencies {
        val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        "detektPlugins"(catalog.findLibrary("detekt-formatting").get())
    }

    // KMP modules need explicit source set configuration for detekt
    afterEvaluate {
        val kmpSourceDirs = listOf(
            "src/commonMain/kotlin",
            "src/androidMain/kotlin",
            "src/iosMain/kotlin",
        ).map { file(it) }.filter { it.exists() }

        if (kmpSourceDirs.isNotEmpty()) {
            tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
                setSource(source + project.files(kmpSourceDirs))
            }
        }
    }
}
