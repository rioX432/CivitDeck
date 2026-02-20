package com.riox432.civitdeck.domain.model

import com.riox432.civitdeck.data.local.entity.CollectionModelEntity

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

fun CollectionModelEntity.toFavoriteModelSummary(): FavoriteModelSummary = FavoriteModelSummary(
    id = modelId,
    name = name,
    type = ModelType.entries.find { it.name == type } ?: ModelType.Checkpoint,
    nsfw = nsfw,
    thumbnailUrl = thumbnailUrl,
    creatorName = creatorName,
    downloadCount = downloadCount,
    favoriteCount = favoriteCount,
    rating = rating,
    favoritedAt = addedAt,
)

fun Model.toCollectionModelEntry(collectionId: Long, timestamp: Long): CollectionModelEntity =
    CollectionModelEntity(
        collectionId = collectionId,
        modelId = id,
        name = name,
        type = type.name,
        nsfw = nsfw,
        thumbnailUrl = modelVersions.firstOrNull()?.images?.firstOrNull()?.url,
        creatorName = creator?.username,
        downloadCount = stats.downloadCount,
        favoriteCount = stats.favoriteCount,
        rating = stats.rating,
        addedAt = timestamp,
    )
