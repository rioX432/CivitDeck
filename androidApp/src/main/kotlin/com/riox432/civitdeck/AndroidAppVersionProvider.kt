package com.riox432.civitdeck

import com.riox432.civitdeck.domain.repository.AppVersionProvider

class AndroidAppVersionProvider : AppVersionProvider {
    override fun getVersionName(): String = BuildConfig.VERSION_NAME
}
