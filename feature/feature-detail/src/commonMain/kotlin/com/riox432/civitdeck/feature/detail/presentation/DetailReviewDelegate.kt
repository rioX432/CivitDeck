package com.riox432.civitdeck.feature.detail.presentation

import com.riox432.civitdeck.domain.model.ResourceReview
import com.riox432.civitdeck.domain.model.ReviewSortOrder
import com.riox432.civitdeck.domain.usecase.GetModelReviewsUseCase
import com.riox432.civitdeck.domain.usecase.GetRatingTotalsUseCase
import com.riox432.civitdeck.domain.usecase.SubmitReviewUseCase
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class DetailReviewDelegate(
    private val modelId: Long,
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<ModelDetailUiState>,
    private val getModelReviewsUseCase: GetModelReviewsUseCase,
    private val getRatingTotalsUseCase: GetRatingTotalsUseCase,
    private val submitReviewUseCase: SubmitReviewUseCase,
) {

    fun loadReviews() {
        scope.launch {
            uiState.update { it.copy(isReviewsLoading = true, reviewsError = null) }
            suspendRunCatching {
                val totals = getRatingTotalsUseCase(modelId)
                val page = getModelReviewsUseCase(modelId)
                totals to sortReviews(page.items, uiState.value.reviewSortOrder)
            }
                .onSuccess { (totals, sorted) ->
                    uiState.update {
                        it.copy(
                            reviews = sorted,
                            ratingTotals = totals,
                            isReviewsLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    uiState.update {
                        it.copy(isReviewsLoading = false, reviewsError = e.message)
                    }
                }
        }
    }

    fun onReviewSortChanged(order: ReviewSortOrder) {
        uiState.update { it.copy(reviewSortOrder = order) }
        loadReviews()
    }

    @Suppress("LongParameterList")
    fun submitReview(
        modelVersionId: Long,
        rating: Int,
        recommended: Boolean,
        details: String?,
    ) {
        scope.launch {
            uiState.update { it.copy(isSubmittingReview = true) }
            suspendRunCatching {
                submitReviewUseCase(modelId, modelVersionId, rating, recommended, details)
            }
                .onSuccess {
                    uiState.update {
                        it.copy(isSubmittingReview = false, reviewSubmitSuccess = true)
                    }
                    loadReviews()
                }
                .onFailure { e ->
                    Logger.w(TAG, "Submit review failed: ${e.message}")
                    uiState.update { it.copy(isSubmittingReview = false) }
                }
        }
    }

    fun dismissReviewSuccess() {
        uiState.update { it.copy(reviewSubmitSuccess = false) }
    }

    private fun sortReviews(
        reviews: List<ResourceReview>,
        order: ReviewSortOrder,
    ): List<ResourceReview> = when (order) {
        ReviewSortOrder.Newest -> reviews.sortedByDescending { it.createdAt }
        ReviewSortOrder.HighestRated -> reviews.sortedByDescending { it.rating }
        ReviewSortOrder.LowestRated -> reviews.sortedBy { it.rating }
    }
}

private const val TAG = "DetailReviewDelegate"
