package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.UserPreferencesRepository

class SetApiKeyUseCase(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(apiKey: String?) = repository.setApiKey(apiKey)
}
