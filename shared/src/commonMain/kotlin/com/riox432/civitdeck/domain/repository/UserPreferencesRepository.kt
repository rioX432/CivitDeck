package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.PollingInterval
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface UserPreferencesRepository {
    fun observeNsfwFilterLevel(): Flow<NsfwFilterLevel>
    suspend fun setNsfwFilterLevel(level: NsfwFilterLevel)
    fun observeDefaultSortOrder(): Flow<SortOrder>
    suspend fun setDefaultSortOrder(sort: SortOrder)
    fun observeDefaultTimePeriod(): Flow<TimePeriod>
    suspend fun setDefaultTimePeriod(period: TimePeriod)
    fun observeGridColumns(): Flow<Int>
    suspend fun setGridColumns(columns: Int)
    fun observeApiKey(): Flow<String?>
    suspend fun setApiKey(apiKey: String?)
    suspend fun getApiKey(): String?
    fun observePowerUserMode(): Flow<Boolean>
    suspend fun setPowerUserMode(enabled: Boolean)
    fun observeNotificationsEnabled(): Flow<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean)
    fun observePollingInterval(): Flow<PollingInterval>
    suspend fun setPollingInterval(interval: PollingInterval)
    fun observeNsfwBlurSettings(): Flow<NsfwBlurSettings>
    suspend fun setNsfwBlurSettings(settings: NsfwBlurSettings)
    fun observeOfflineCacheEnabled(): Flow<Boolean>
    suspend fun setOfflineCacheEnabled(enabled: Boolean)
    fun observeCacheSizeLimitMb(): Flow<Int>
    suspend fun setCacheSizeLimitMb(limitMb: Int)
}
