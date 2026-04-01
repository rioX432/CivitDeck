package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.domain.repository.AuthRepository
import com.riox432.civitdeck.domain.util.suspendRunCatching

class AuthRepositoryImpl(private val api: CivitAiApi) : AuthRepository {
    override suspend fun validateApiKey(apiKey: String): Result<String> {
        return suspendRunCatching { api.getMe(apiKey).username }
    }
}
