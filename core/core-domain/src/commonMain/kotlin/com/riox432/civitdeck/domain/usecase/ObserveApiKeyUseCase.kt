package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.AuthPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveApiKeyUseCase(private val repository: AuthPreferencesRepository) {
    operator fun invoke(): Flow<String?> = repository.observeApiKey()
}
