package com.riox432.civitdeck.domain.model

data class FeedItem(
    val modelId: Long,
    val creatorUsername: String,
    val title: String,
    val thumbnailUrl: String?,
    val type: ModelType,
    val publishedAt: String,
    val isUnread: Boolean,
)
