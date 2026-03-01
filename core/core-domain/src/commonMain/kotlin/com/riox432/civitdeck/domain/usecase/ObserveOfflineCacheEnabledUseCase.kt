package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.StoragePreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveOfflineCacheEnabledUseCase(
    private val repository: StoragePreferencesRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeOfflineCacheEnabled()
}
