package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.CacheRepository

class EvictCacheUseCase(private val repository: CacheRepository) {
    suspend operator fun invoke(maxBytes: Long) {
        repository.evictToSize(maxBytes)
    }
}
