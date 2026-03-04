pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "CivitDeck"
include(":shared")
include(":androidApp")
include(":core:core-domain")
include(":core:core-network")
include(":core:core-database")
include(":core:core-ui")
include(":feature:feature-settings")
include(":feature:feature-creator")
include(":feature:feature-collections")
include(":feature:feature-prompts")
include(":feature:feature-gallery")
include(":feature:feature-detail")
include(":feature:feature-search")
include(":feature:feature-comfyui")
include(":feature:feature-externalserver")
