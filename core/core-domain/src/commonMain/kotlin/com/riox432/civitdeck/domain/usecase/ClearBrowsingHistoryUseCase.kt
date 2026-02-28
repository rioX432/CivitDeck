package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository

class ClearBrowsingHistoryUseCase(private val repository: BrowsingHistoryRepository) {
    suspend operator fun invoke() = repository.clearAll()
}
