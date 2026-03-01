package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.PollingInterval
import kotlinx.coroutines.flow.Flow

interface AppBehaviorPreferencesRepository {
    fun observePowerUserMode(): Flow<Boolean>
    suspend fun setPowerUserMode(enabled: Boolean)
    fun observeNotificationsEnabled(): Flow<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean)
    fun observePollingInterval(): Flow<PollingInterval>
    suspend fun setPollingInterval(interval: PollingInterval)
    fun observeSeenTutorialVersion(): Flow<Int>
    suspend fun setSeenTutorialVersion(version: Int)
}
