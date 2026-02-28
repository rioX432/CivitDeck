package com.riox432.civitdeck.domain.repository

interface AuthRepository {
    suspend fun validateApiKey(apiKey: String): Result<String>
}
