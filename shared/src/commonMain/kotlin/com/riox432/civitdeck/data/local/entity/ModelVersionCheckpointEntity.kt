package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_version_checkpoints")
data class ModelVersionCheckpointEntity(
    @PrimaryKey val modelId: Long,
    val lastKnownVersionId: Long,
    val lastCheckedAt: Long,
)
