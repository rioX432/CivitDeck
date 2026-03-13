package com.riox432.civitdeck.plugin

import kotlinx.coroutines.flow.Flow

/**
 * Specialized plugin interface for dataset export format integrations.
 * Plugins implement this to provide custom export formats (e.g. kohya-ss ZIP, JSONL, etc.).
 */
interface ExportFormatPlugin : Plugin {
    val supportedFormats: List<PluginExportFormat>

    /**
     * Export a dataset in the specified format.
     *
     * @param datasetId the dataset collection to export
     * @param formatId one of the format IDs from [supportedFormats]
     * @param options optional key-value pairs for configurable export parameters
     * @return a flow of [PluginExportProgress] events
     */
    fun export(
        datasetId: Long,
        formatId: String,
        options: Map<String, String> = emptyMap(),
    ): Flow<PluginExportProgress>
}

/**
 * Describes a single export format that a plugin can produce.
 */
data class PluginExportFormat(
    val id: String,
    val name: String,
    val description: String,
    val fileExtension: String,
    val mimeType: String,
)

/**
 * Progress events emitted during a plugin-driven export.
 * Intentionally separate from the domain ExportProgress to keep
 * core-plugin independent of core-domain.
 */
sealed interface PluginExportProgress {
    data object Preparing : PluginExportProgress
    data class Downloading(val current: Int, val total: Int) : PluginExportProgress
    data object WritingManifest : PluginExportProgress
    data class Completed(val outputPath: String, val warningCount: Int) : PluginExportProgress
    data class Failed(val message: String) : PluginExportProgress
}
