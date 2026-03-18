package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository

class TrackModelViewUseCase(private val repository: BrowsingHistoryRepository) {
    suspend operator fun invoke(
        modelId: Long,
        modelName: String,
        modelType: String,
        creatorName: String?,
        thumbnailUrl: String?,
        tags: List<String>,
    ) = repository.trackView(modelId, modelName, modelType, creatorName, thumbnailUrl, tags)
}
