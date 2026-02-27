package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.LocalCacheDataSource
import com.riox432.civitdeck.domain.model.CacheInfo
import com.riox432.civitdeck.domain.repository.CacheRepository

class CacheRepositoryImpl(private val dataSource: LocalCacheDataSource) : CacheRepository {
    override suspend fun clearAll() = dataSource.clearAll()

    override suspend fun getCacheInfo(): CacheInfo {
        return CacheInfo(
            sizeBytes = dataSource.getCacheSizeBytes(),
            entryCount = dataSource.getEntryCount(),
        )
    }

    override suspend fun evictToSize(maxBytes: Long) = dataSource.evictToSize(maxBytes)
}
