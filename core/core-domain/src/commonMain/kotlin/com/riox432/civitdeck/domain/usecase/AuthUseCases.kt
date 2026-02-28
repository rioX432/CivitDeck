package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.AuthRepository
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveApiKeyUseCase(private val repository: UserPreferencesRepository) {
    operator fun invoke(): Flow<String?> = repository.observeApiKey()
}

class SetApiKeyUseCase(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(apiKey: String?) = repository.setApiKey(apiKey)
}

class ValidateApiKeyUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(apiKey: String): Result<String> {
        return repository.validateApiKey(apiKey)
    }
}
