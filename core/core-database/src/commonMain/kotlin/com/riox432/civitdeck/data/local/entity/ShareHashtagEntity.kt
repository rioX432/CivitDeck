package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "share_hashtags")
data class ShareHashtagEntity(
    @PrimaryKey val tag: String,
    val isEnabled: Boolean,
    val isCustom: Boolean,
    val addedAt: Long,
)
