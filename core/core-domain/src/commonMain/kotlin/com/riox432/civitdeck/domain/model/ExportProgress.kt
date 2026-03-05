package com.riox432.civitdeck.domain.model

sealed interface ExportProgress {
    data object Preparing : ExportProgress
    data class Downloading(val current: Int, val total: Int) : ExportProgress
    data object WritingManifest : ExportProgress
    data class Completed(val outputPath: String, val warningCount: Int) : ExportProgress
    data class Failed(val message: String) : ExportProgress
}
