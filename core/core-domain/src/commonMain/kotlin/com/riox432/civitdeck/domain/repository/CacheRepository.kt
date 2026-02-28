package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.CacheInfo

interface CacheRepository {
    suspend fun clearAll()
    suspend fun getCacheInfo(): CacheInfo
    suspend fun evictToSize(maxBytes: Long)
}
