package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "image_tags",
    foreignKeys = [
        ForeignKey(
            entity = DatasetImageEntity::class,
            parentColumns = ["id"],
            childColumns = ["datasetImageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("datasetImageId")],
)
data class ImageTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val datasetImageId: Long,
    val tag: String,
)
