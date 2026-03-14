package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val nsfwFilterLevel: String = "Off",
    val defaultSortOrder: String = "MostDownloaded",
    val defaultTimePeriod: String = "AllTime",
    val gridColumns: Int = 2,
    val apiKey: String? = null,
    val powerUserMode: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val pollingIntervalMinutes: Int = 0,
    val nsfwBlurSoft: Int = 75,
    val nsfwBlurMature: Int = 25,
    val nsfwBlurExplicit: Int = 0,
    val offlineCacheEnabled: Boolean = true,
    val cacheSizeLimitMb: Int = DEFAULT_CACHE_SIZE_LIMIT_MB,
    val accentColor: String = "Blue",
    val amoledDarkMode: Boolean = false,
    val seenTutorialVersion: Int = 0,
    val civitaiLinkKey: String? = null,
    val themeMode: String = "SYSTEM",
    val customNavShortcuts: String = "",
    val feedQualityThreshold: Int = DEFAULT_FEED_QUALITY_THRESHOLD,
) {
    companion object {
        const val DEFAULT_CACHE_SIZE_LIMIT_MB = 200
        const val DEFAULT_FEED_QUALITY_THRESHOLD = 30
    }
}
