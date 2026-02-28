package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.CacheRepository

class ClearCacheUseCase(private val repository: CacheRepository) {
    suspend operator fun invoke() = repository.clearAll()
}
