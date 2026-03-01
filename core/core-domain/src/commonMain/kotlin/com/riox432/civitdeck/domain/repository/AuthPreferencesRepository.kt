package com.riox432.civitdeck.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthPreferencesRepository {
    fun observeApiKey(): Flow<String?>
    suspend fun setApiKey(apiKey: String?)
    suspend fun getApiKey(): String?
    fun observeCivitaiLinkKey(): Flow<String?>
    suspend fun setCivitaiLinkKey(key: String?)
    suspend fun getCivitaiLinkKey(): String?
}
