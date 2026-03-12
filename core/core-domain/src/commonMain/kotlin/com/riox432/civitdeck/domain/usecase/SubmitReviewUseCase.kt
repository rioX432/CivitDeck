package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ReviewRepository

class SubmitReviewUseCase(private val repository: ReviewRepository) {
    suspend operator fun invoke(
        modelId: Long,
        modelVersionId: Long,
        rating: Int,
        recommended: Boolean,
        details: String?,
    ) = repository.submitReview(modelId, modelVersionId, rating, recommended, details)
}
