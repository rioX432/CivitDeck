package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_models")
data class HiddenModelEntity(
    @PrimaryKey val modelId: Long,
    val modelName: String,
    val hiddenAt: Long,
)
