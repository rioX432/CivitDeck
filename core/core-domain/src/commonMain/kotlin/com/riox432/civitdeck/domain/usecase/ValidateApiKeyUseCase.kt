package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.AuthRepository

class ValidateApiKeyUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(apiKey: String): Result<String> {
        return repository.validateApiKey(apiKey)
    }
}
