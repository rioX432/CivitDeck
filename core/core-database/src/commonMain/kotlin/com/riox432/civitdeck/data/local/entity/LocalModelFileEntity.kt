package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_model_files",
    foreignKeys = [
        ForeignKey(
            entity = ModelDirectoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["directoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["directoryId"]),
        Index(value = ["sha256Hash"]),
    ],
)
data class LocalModelFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val directoryId: Long,
    val filePath: String,
    val fileName: String,
    val sha256Hash: String,
    val sizeBytes: Long,
    val scannedAt: Long,
    val matchedModelId: Long? = null,
    val matchedModelName: String? = null,
    val matchedVersionId: Long? = null,
    val matchedVersionName: String? = null,
    val latestVersionId: Long? = null,
    val hasUpdate: Boolean = false,
)
