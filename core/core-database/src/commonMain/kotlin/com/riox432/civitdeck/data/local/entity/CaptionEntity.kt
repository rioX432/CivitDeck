package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "captions",
    foreignKeys = [
        ForeignKey(
            entity = DatasetImageEntity::class,
            parentColumns = ["id"],
            childColumns = ["datasetImageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CaptionEntity(
    @PrimaryKey
    val datasetImageId: Long,
    val text: String,
)
