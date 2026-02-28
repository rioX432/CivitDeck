package com.riox432.civitdeck.domain.repository

import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    fun observeRecentSearches(): Flow<List<String>>
    suspend fun addSearch(query: String)
    suspend fun deleteSearch(query: String)
    suspend fun clearAll()
}
