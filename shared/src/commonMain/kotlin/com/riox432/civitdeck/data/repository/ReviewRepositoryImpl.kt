package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.ApiKeyProvider
import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.domain.model.RatingTotals
import com.riox432.civitdeck.domain.model.ResourceReview
import com.riox432.civitdeck.domain.repository.ReviewPage
import com.riox432.civitdeck.domain.repository.ReviewRepository

class ReviewRepositoryImpl(
    private val api: CivitAiApi,
    private val apiKeyProvider: ApiKeyProvider,
) : ReviewRepository {

    override suspend fun getReviews(
        modelId: Long,
        modelVersionId: Long?,
        limit: Int,
        cursor: Int?,
    ): ReviewPage {
        val response = api.getReviews(modelId, modelVersionId, limit, cursor)
        return ReviewPage(
            items = response.items.map { dto ->
                ResourceReview(
                    id = dto.id,
                    modelId = dto.modelId,
                    modelVersionId = dto.modelVersionId,
                    rating = dto.rating,
                    recommended = dto.recommended,
                    details = dto.details,
                    createdAt = dto.createdAt,
                    username = dto.user?.username,
                    userImage = dto.user?.image,
                    commentCount = dto.thread?.commentCount ?: 0,
                )
            },
            nextCursor = response.nextCursor,
        )
    }

    override suspend fun getRatingTotals(
        modelId: Long,
        modelVersionId: Long?,
    ): RatingTotals {
        val response = api.getRatingTotals(modelId, modelVersionId)
        return RatingTotals(
            star1 = response.star1,
            star2 = response.star2,
            star3 = response.star3,
            star4 = response.star4,
            star5 = response.star5,
            thumbsUp = response.up,
            thumbsDown = response.down,
        )
    }

    override suspend fun submitReview(
        modelId: Long,
        modelVersionId: Long,
        rating: Int,
        recommended: Boolean,
        details: String?,
    ) {
        val apiKey = checkNotNull(apiKeyProvider.apiKey) {
            "API key required to submit reviews"
        }
        api.createReview(apiKey, modelId, modelVersionId, rating, recommended, details)
    }
}
