package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "feed_cache",
    indices = [
        Index("creatorUsername"),
        Index("publishedAt"),
    ],
)
data class FeedCacheEntity(
    @PrimaryKey val modelId: Long,
    val creatorUsername: String,
    val title: String,
    val thumbnailUrl: String?,
    val type: String,
    val publishedAt: String,
    val cachedAt: Long,
)
