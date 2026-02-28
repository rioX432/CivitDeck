package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.UserPreferencesRepository

class SetCacheSizeLimitUseCase(
    private val repository: UserPreferencesRepository,
) {
    suspend operator fun invoke(limitMb: Int) = repository.setCacheSizeLimitMb(limitMb)
}
