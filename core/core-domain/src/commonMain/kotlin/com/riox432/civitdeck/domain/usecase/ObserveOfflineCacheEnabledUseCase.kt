package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveOfflineCacheEnabledUseCase(
    private val repository: UserPreferencesRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeOfflineCacheEnabled()
}
