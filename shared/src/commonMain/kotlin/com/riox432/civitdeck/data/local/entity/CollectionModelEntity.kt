package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "collection_model_entries",
    primaryKeys = ["collectionId", "modelId"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("modelId"),
        Index("collectionId"),
    ],
)
data class CollectionModelEntity(
    val collectionId: Long,
    val modelId: Long,
    val name: String,
    val type: String,
    val nsfw: Boolean,
    val thumbnailUrl: String?,
    val creatorName: String?,
    val downloadCount: Int,
    val favoriteCount: Int,
    val rating: Double,
    val addedAt: Long,
)
