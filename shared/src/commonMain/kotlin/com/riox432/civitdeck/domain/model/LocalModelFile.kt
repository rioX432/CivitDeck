package com.riox432.civitdeck.domain.model

data class ModelDirectory(
    val id: Long,
    val path: String,
    val label: String?,
    val lastScannedAt: Long?,
    val isEnabled: Boolean,
)

data class LocalModelFile(
    val id: Long,
    val directoryId: Long,
    val filePath: String,
    val fileName: String,
    val sha256Hash: String,
    val sizeBytes: Long,
    val scannedAt: Long,
    val matchedModel: MatchedModelInfo?,
)

data class MatchedModelInfo(
    val modelId: Long,
    val modelName: String,
    val versionId: Long,
    val versionName: String,
    val latestVersionId: Long?,
    val hasUpdate: Boolean,
)

enum class ScanStatus {
    Idle,
    Scanning,
    Verifying,
    Completed,
    Error,
}
