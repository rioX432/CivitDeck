package com.riox432.civitdeck.domain.model

data class FavoriteModelSummary(
    val id: Long,
    val name: String,
    val type: ModelType,
    val nsfw: Boolean,
    val thumbnailUrl: String?,
    val creatorName: String?,
    val downloadCount: Int,
    val favoriteCount: Int,
    val rating: Double,
    val favoritedAt: Long,
)
