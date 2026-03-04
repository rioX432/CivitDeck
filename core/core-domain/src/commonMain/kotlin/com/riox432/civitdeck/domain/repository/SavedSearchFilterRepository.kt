package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.SavedSearchFilter
import kotlinx.coroutines.flow.Flow

interface SavedSearchFilterRepository {
    fun observeAll(): Flow<List<SavedSearchFilter>>
    suspend fun save(filter: SavedSearchFilter): Long
    suspend fun delete(id: Long)
}
