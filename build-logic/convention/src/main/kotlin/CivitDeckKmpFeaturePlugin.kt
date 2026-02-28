import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class CivitDeckKmpFeaturePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("civitdeck.kmp.library")

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.dependencies {
                    implementation(project(":core:core-domain"))
                    implementation(project(":core:core-network"))
                    implementation(project(":core:core-database"))
                    val libs = rootProject.extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>()
                        .named("libs")
                    implementation(libs.findLibrary("koin-core").get())
                    implementation(libs.findLibrary("koin-core-viewmodel").get())
                }
            }
        }
    }
}
