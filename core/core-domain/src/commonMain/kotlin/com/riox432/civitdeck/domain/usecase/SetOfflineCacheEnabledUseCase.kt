package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.StoragePreferencesRepository

class SetOfflineCacheEnabledUseCase(
    private val repository: StoragePreferencesRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setOfflineCacheEnabled(enabled)
}
