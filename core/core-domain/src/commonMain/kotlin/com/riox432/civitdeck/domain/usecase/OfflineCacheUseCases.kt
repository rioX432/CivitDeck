package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.CacheInfo
import com.riox432.civitdeck.domain.repository.CacheRepository
import com.riox432.civitdeck.domain.repository.NetworkRepository
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveNetworkStatusUseCase(private val repository: NetworkRepository) {
    operator fun invoke(): Flow<Boolean> = repository.isOnline
}

class GetCacheInfoUseCase(private val repository: CacheRepository) {
    suspend operator fun invoke(): CacheInfo = repository.getCacheInfo()
}

class EvictCacheUseCase(private val repository: CacheRepository) {
    suspend operator fun invoke(maxBytes: Long) {
        repository.evictToSize(maxBytes)
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
