package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "personal_tags",
    indices = [Index("modelId"), Index("tag")],
)
data class PersonalTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modelId: Long,
    val tag: String,
    val addedAt: Long,
)
