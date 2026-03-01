package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.AuthPreferencesRepository

class SetApiKeyUseCase(private val repository: AuthPreferencesRepository) {
    suspend operator fun invoke(apiKey: String?) = repository.setApiKey(apiKey)
}
