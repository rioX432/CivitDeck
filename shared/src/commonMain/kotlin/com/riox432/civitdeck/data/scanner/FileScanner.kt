package com.riox432.civitdeck.data.scanner

data class ScannedFile(
    val filePath: String,
    val fileName: String,
    val sha256Hash: String,
    val sizeBytes: Long,
)

expect class FileScanner() {
    suspend fun scanDirectory(
        path: String,
        onProgress: (current: Int, total: Int) -> Unit,
    ): List<ScannedFile>
}

internal val MODEL_FILE_EXTENSIONS = setOf(
    "safetensors",
    "ckpt",
    "pt",
    "pth",
    "bin",
)
