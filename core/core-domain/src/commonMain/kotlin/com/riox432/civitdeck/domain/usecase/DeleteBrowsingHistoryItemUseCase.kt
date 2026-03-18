package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository

class DeleteBrowsingHistoryItemUseCase(private val repository: BrowsingHistoryRepository) {
    suspend operator fun invoke(historyId: Long) = repository.deleteById(historyId)
}
