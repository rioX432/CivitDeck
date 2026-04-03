package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caches computed quality scores for models to avoid re-calculation on each render.
 * Scores are refreshed when model stats change (TTL-based eviction).
 */
@Entity(tableName = "quality_score_cache")
data class QualityScoreCacheEntity(
    @PrimaryKey val modelId: Long,
    val score: Int,
    val downloadCount: Int,
    val favoriteCount: Int,
    val ratingCount: Int,
    val cachedAt: Long,
)
