import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class CivitDeckKmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.kotlin.multiplatform.library")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                jvm()
                iosArm64()
                iosSimulatorArm64()

                // The new com.android.kotlin.multiplatform.library plugin replaces
                // androidTarget() + the top-level android {} block. The Android target
                // is configured here; each module sets its own namespace.
                targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
                    .configureEach {
                        compileSdk = 36
                        minSdk = 29
                        compilerOptions {
                            jvmTarget.set(JvmTarget.JVM_17)
                        }
                        // Host (JVM) unit tests are opt-in under the new plugin.
                        // commonTest runs on the androidHostTest compilation.
                        withHostTest {
                            isIncludeAndroidResources = true
                        }
                    }
            }
        }
    }
}
