package com.riox432.civitdeck.domain.repository

/**
 * Provides the current app version string.
 * Implemented per-platform (Android reads BuildConfig, Desktop reads build.gradle version).
 */
interface AppVersionProvider {
    fun getVersionName(): String
}
