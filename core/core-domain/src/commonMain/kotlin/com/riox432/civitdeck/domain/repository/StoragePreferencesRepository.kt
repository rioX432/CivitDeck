package com.riox432.civitdeck.domain.repository

import kotlinx.coroutines.flow.Flow

interface StoragePreferencesRepository {
    fun observeOfflineCacheEnabled(): Flow<Boolean>
    suspend fun setOfflineCacheEnabled(enabled: Boolean)
    fun observeCacheSizeLimitMb(): Flow<Int>
    suspend fun setCacheSizeLimitMb(limitMb: Int)
}
