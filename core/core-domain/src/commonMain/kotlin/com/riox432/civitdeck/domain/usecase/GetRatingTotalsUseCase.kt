package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.RatingTotals
import com.riox432.civitdeck.domain.repository.ReviewRepository

class GetRatingTotalsUseCase(private val repository: ReviewRepository) {
    suspend operator fun invoke(
        modelId: Long,
        modelVersionId: Long? = null,
    ): RatingTotals = repository.getRatingTotals(modelId, modelVersionId)
}
