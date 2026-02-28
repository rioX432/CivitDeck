package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow

class ObserveSearchHistoryUseCase(private val repository: SearchHistoryRepository) {
    operator fun invoke(): Flow<List<String>> = repository.observeRecentSearches()
}

class AddSearchHistoryUseCase(private val repository: SearchHistoryRepository) {
    suspend operator fun invoke(query: String) = repository.addSearch(query)
}

class ClearSearchHistoryUseCase(private val repository: SearchHistoryRepository) {
    suspend operator fun invoke() = repository.clearAll()
}
