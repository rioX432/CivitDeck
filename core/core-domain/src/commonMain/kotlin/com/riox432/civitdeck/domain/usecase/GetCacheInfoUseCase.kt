package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.CacheInfo
import com.riox432.civitdeck.domain.repository.CacheRepository

class GetCacheInfoUseCase(private val repository: CacheRepository) {
    suspend operator fun invoke(): CacheInfo = repository.getCacheInfo()
}
