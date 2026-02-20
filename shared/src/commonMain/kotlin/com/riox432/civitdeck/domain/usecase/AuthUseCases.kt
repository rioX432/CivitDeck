package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveApiKeyUseCase(private val repository: UserPreferencesRepository) {
    operator fun invoke(): Flow<String?> = repository.observeApiKey()
}

class SetApiKeyUseCase(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(apiKey: String?) = repository.setApiKey(apiKey)
}

class ValidateApiKeyUseCase(private val api: CivitAiApi) {
    suspend operator fun invoke(apiKey: String): String {
        return api.getMe(apiKey).username
    }
}
