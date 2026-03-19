package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "model_update_notifications",
    indices = [
        Index("createdAt"),
        Index("isRead"),
    ],
)
data class ModelUpdateNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modelId: Long,
    val modelName: String,
    val newVersionName: String,
    val newVersionId: Long,
    val source: String,
    val createdAt: Long,
    val isRead: Boolean = false,
)
