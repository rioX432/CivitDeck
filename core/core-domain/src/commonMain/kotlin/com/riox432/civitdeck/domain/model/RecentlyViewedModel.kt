package com.riox432.civitdeck.domain.model

data class RecentlyViewedModel(
    val historyId: Long,
    val modelId: Long,
    val modelName: String,
    val modelType: String,
    val creatorName: String?,
    val thumbnailUrl: String?,
    val viewedAt: Long,
)
