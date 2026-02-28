package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "excluded_tags")
data class ExcludedTagEntity(
    @PrimaryKey val tag: String,
    val addedAt: Long,
)
