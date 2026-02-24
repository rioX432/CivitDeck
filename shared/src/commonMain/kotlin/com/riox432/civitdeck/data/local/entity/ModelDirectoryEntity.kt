package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "model_directories")
data class ModelDirectoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val label: String? = null,
    val lastScannedAt: Long? = null,
    val isEnabled: Boolean = true,
)
