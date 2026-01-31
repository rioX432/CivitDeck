package com.omooooori.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_api_responses")
data class CachedApiResponseEntity(
    @PrimaryKey
    val cacheKey: String,
    val responseJson: String,
    val cachedAt: Long,
)
