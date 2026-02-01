package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_models")
data class FavoriteModelEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val type: String,
    val nsfw: Boolean,
    val thumbnailUrl: String?,
    val creatorName: String?,
    val downloadCount: Int,
    val favoriteCount: Int,
    val rating: Double,
    val favoritedAt: Long,
)
