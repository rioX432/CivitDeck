package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.SearchHistoryRepository

class ClearSearchHistoryUseCase(private val repository: SearchHistoryRepository) {
    suspend operator fun invoke() = repository.clearAll()
}
