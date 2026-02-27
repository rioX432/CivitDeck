package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.domain.repository.AuthRepository

class AuthRepositoryImpl(private val api: CivitAiApi) : AuthRepository {
    override suspend fun validateApiKey(apiKey: String): String {
        return api.getMe(apiKey).username
    }
}
