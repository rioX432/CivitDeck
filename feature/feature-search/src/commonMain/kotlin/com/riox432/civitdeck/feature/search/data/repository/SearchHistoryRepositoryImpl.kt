package com.riox432.civitdeck.feature.search.data.repository

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.SearchHistoryDao
import com.riox432.civitdeck.data.local.entity.SearchHistoryEntity
import com.riox432.civitdeck.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchHistoryRepositoryImpl(
    private val dao: SearchHistoryDao,
) : SearchHistoryRepository {

    override fun observeRecentSearches(): Flow<List<String>> =
        dao.observeRecent().map { entities -> entities.map { it.query } }

    override suspend fun addSearch(query: String) {
        dao.deleteByQuery(query)
        dao.insert(SearchHistoryEntity(query = query, searchedAt = currentTimeMillis()))
    }

    override suspend fun deleteSearch(query: String) {
        dao.deleteByQuery(query)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}
