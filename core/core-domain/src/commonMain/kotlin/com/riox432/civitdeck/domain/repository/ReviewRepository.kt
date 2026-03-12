package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.RatingTotals
import com.riox432.civitdeck.domain.model.ResourceReview

interface ReviewRepository {
    suspend fun getReviews(
        modelId: Long,
        modelVersionId: Long? = null,
        limit: Int = 20,
        cursor: Int? = null,
    ): ReviewPage

    suspend fun getRatingTotals(
        modelId: Long,
        modelVersionId: Long? = null,
    ): RatingTotals

    suspend fun submitReview(
        modelId: Long,
        modelVersionId: Long,
        rating: Int,
        recommended: Boolean,
        details: String?,
    )
}

data class ReviewPage(
    val items: List<ResourceReview>,
    val nextCursor: Int?,
)
