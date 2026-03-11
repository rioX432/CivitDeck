package com.riox432.civitdeck.domain.model

enum class DownloadStatus { Pending, Downloading, Paused, Completed, Failed, Cancelled }

data class ModelDownload(
    val id: Long = 0,
    val modelId: Long,
    val modelName: String,
    val versionId: Long,
    val versionName: String,
    val fileId: Long,
    val fileName: String,
    val fileUrl: String,
    val fileSizeBytes: Long,
    val downloadedBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.Pending,
    val modelType: String,
    val destinationPath: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
