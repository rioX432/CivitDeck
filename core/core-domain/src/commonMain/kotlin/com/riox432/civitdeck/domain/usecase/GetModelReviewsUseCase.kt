package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ReviewPage
import com.riox432.civitdeck.domain.repository.ReviewRepository

class GetModelReviewsUseCase(private val repository: ReviewRepository) {
    suspend operator fun invoke(
        modelId: Long,
        modelVersionId: Long? = null,
        limit: Int = 20,
        cursor: Int? = null,
    ): ReviewPage = repository.getReviews(modelId, modelVersionId, limit, cursor)
}
