package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.data.local.LocalCacheDataSource
import com.riox432.civitdeck.data.local.NetworkMonitor
import com.riox432.civitdeck.domain.model.CacheInfo
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveNetworkStatusUseCase(private val networkMonitor: NetworkMonitor) {
    operator fun invoke(): Flow<Boolean> = networkMonitor.isOnline
}

class GetCacheInfoUseCase(private val cacheDataSource: LocalCacheDataSource) {
    suspend operator fun invoke(): CacheInfo {
        val sizeBytes = cacheDataSource.getCacheSizeBytes()
        val entryCount = cacheDataSource.getEntryCount()
        return CacheInfo(sizeBytes = sizeBytes, entryCount = entryCount)
    }
}

class EvictCacheUseCase(private val cacheDataSource: LocalCacheDataSource) {
    suspend operator fun invoke(maxBytes: Long) {
        cacheDataSource.evictToSize(maxBytes)
    }
}

class ObserveOfflineCacheEnabledUseCase(
    private val repository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeOfflineCacheEnabled()
}

class SetOfflineCacheEnabledUseCase(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setOfflineCacheEnabled(enabled)
}

class ObserveCacheSizeLimitUseCase(
    private val repository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<Int> = repository.observeCacheSizeLimitMb()
}

class SetCacheSizeLimitUseCase(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke(limitMb: Int) = repository.setCacheSizeLimitMb(limitMb)
}
