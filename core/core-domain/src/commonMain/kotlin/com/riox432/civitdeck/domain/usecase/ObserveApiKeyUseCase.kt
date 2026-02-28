package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveApiKeyUseCase(private val repository: UserPreferencesRepository) {
    operator fun invoke(): Flow<String?> = repository.observeApiKey()
}
