package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository

class TrackModelViewUseCase(private val repository: BrowsingHistoryRepository) {
    suspend operator fun invoke(
        modelId: Long,
        modelType: String,
        creatorName: String?,
        tags: List<String>,
    ) = repository.trackView(modelId, modelType, creatorName, tags)
}
