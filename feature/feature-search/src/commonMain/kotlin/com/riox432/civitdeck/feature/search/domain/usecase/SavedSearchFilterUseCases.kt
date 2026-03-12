package com.riox432.civitdeck.feature.search.domain.usecase

import com.riox432.civitdeck.domain.model.SavedSearchFilter
import com.riox432.civitdeck.domain.repository.SavedSearchFilterRepository
import com.riox432.civitdeck.domain.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow

class ObserveSavedSearchFiltersUseCase(private val repository: SavedSearchFilterRepository) {
    operator fun invoke(): Flow<List<SavedSearchFilter>> = repository.observeAll()
}

class SaveSearchFilterUseCase(private val repository: SavedSearchFilterRepository) {
    suspend operator fun invoke(name: String, filter: SavedSearchFilter): Long =
        repository.save(filter.copy(id = 0, name = name, savedAt = currentTimeMillis()))
}

class DeleteSavedSearchFilterUseCase(private val repository: SavedSearchFilterRepository) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
