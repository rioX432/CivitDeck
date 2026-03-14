package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.NavShortcut
import com.riox432.civitdeck.domain.model.PollingInterval
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface AppBehaviorPreferencesRepository {
    fun observePowerUserMode(): Flow<Boolean>
    suspend fun setPowerUserMode(enabled: Boolean)
    fun observeNotificationsEnabled(): Flow<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean)
    fun observePollingInterval(): Flow<PollingInterval>
    suspend fun setPollingInterval(interval: PollingInterval)
    fun observeSeenTutorialVersion(): Flow<Int>
    suspend fun setSeenTutorialVersion(version: Int)
    fun observeCustomNavShortcuts(): Flow<List<NavShortcut>>
    suspend fun setCustomNavShortcuts(items: List<NavShortcut>)
    fun observeFeedQualityThreshold(): Flow<Int>
    suspend fun setFeedQualityThreshold(threshold: Int)
}
