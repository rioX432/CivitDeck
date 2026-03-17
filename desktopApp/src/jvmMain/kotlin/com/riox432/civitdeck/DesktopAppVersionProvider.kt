package com.riox432.civitdeck

import com.riox432.civitdeck.domain.repository.AppVersionProvider

class DesktopAppVersionProvider : AppVersionProvider {
    override fun getVersionName(): String = APP_VERSION

    companion object {
        // Keep in sync with desktopApp/build.gradle.kts packageVersion
        const val APP_VERSION = "2.0.0"
    }
}
