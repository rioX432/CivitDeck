package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository

class GetViewedModelIdsUseCase(private val repository: BrowsingHistoryRepository) {
    suspend operator fun invoke(): Set<Long> = repository.getAllViewedModelIds()
}
