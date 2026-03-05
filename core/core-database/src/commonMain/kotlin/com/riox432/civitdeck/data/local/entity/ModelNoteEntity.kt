package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "model_notes",
    indices = [Index("modelId")],
)
data class ModelNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modelId: Long,
    val noteText: String,
    val createdAt: Long,
    val updatedAt: Long,
)
