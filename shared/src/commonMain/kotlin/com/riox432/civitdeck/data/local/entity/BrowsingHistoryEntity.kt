package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "browsing_history")
data class BrowsingHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modelId: Long,
    val modelType: String,
    val creatorName: String?,
    val tags: String,
    val viewedAt: Long,
)
