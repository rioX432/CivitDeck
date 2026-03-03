package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dataset_images",
    foreignKeys = [
        ForeignKey(
            entity = DatasetCollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["datasetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("datasetId")],
)
data class DatasetImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val datasetId: Long,
    val imageUrl: String,
    val sourceType: String,
    val trainable: Boolean = true,
    val addedAt: Long,
    val licenseNote: String? = null,
    val pHash: String? = null,
    val excluded: Boolean = false,
    val width: Int? = null,
    val height: Int? = null,
)
