package com.riox432.civitdeck.domain.model

data class ModelUpdateNotification(
    val id: Long,
    val modelId: Long,
    val modelName: String,
    val newVersionName: String,
    val newVersionId: Long,
    val source: UpdateSource,
    val createdAt: Long,
    val isRead: Boolean,
)

enum class UpdateSource {
    FAVORITE,
    FOLLOWED,
}
