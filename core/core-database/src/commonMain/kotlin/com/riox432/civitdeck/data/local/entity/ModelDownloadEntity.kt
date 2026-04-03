package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "model_downloads",
    indices = [
        Index("modelId"),
        Index("status"),
        Index("createdAt"),
    ],
)
data class ModelDownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modelId: Long,
    val modelName: String,
    val versionId: Long,
    val versionName: String,
    val fileId: Long,
    val fileName: String,
    val fileUrl: String,
    val fileSizeBytes: Long,
    val downloadedBytes: Long = 0,
    val status: String = "Pending",
    val modelType: String,
    val destinationPath: String? = null,
    val errorMessage: String? = null,
    val expectedSha256: String? = null,
    val hashVerified: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
