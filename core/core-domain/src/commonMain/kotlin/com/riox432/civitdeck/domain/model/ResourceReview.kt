package com.riox432.civitdeck.domain.model

data class ResourceReview(
    val id: Long,
    val modelId: Long,
    val modelVersionId: Long,
    val rating: Int,
    val recommended: Boolean,
    val details: String?,
    val createdAt: String,
    val username: String?,
    val userImage: String?,
    val commentCount: Int,
)

data class RatingTotals(
    val star1: Int,
    val star2: Int,
    val star3: Int,
    val star4: Int,
    val star5: Int,
    val thumbsUp: Int,
    val thumbsDown: Int,
) {
    val total: Int get() = star1 + star2 + star3 + star4 + star5

    fun countForStar(star: Int): Int = when (star) {
        1 -> star1
        2 -> star2
        3 -> star3
        4 -> star4
        5 -> star5
        else -> 0
    }
}

enum class ReviewSortOrder {
    Newest,
    HighestRated,
    LowestRated,
}
