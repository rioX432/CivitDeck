package com.riox432.civitdeck.feature.search.domain.usecase

import com.riox432.civitdeck.domain.model.InteractionType
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository

class TrackRecommendationClickUseCase(
    private val browsingHistoryRepository: BrowsingHistoryRepository,
) {
    suspend operator fun invoke(modelId: Long) {
        browsingHistoryRepository.trackInteraction(modelId, InteractionType.RECOMMENDATION_CLICK)
    }
}
