package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.UpdateResult
import kotlinx.coroutines.flow.Flow

interface UpdateRepository {
    suspend fun checkForUpdate(): UpdateResult
    fun observeAutoUpdateCheckEnabled(): Flow<Boolean>
    suspend fun setAutoUpdateCheckEnabled(enabled: Boolean)
    fun observeLastUpdateCheckTimestamp(): Flow<Long>
    suspend fun setLastUpdateCheckTimestamp(timestamp: Long)
}
