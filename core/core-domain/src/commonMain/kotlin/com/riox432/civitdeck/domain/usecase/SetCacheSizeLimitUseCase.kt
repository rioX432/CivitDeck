package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.StoragePreferencesRepository

class SetCacheSizeLimitUseCase(
    private val repository: StoragePreferencesRepository,
) {
    suspend operator fun invoke(limitMb: Int) = repository.setCacheSizeLimitMb(limitMb)
}
