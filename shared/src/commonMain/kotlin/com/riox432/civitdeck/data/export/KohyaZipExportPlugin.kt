package com.riox432.civitdeck.data.export

import com.riox432.civitdeck.domain.model.ExportFormat
import com.riox432.civitdeck.domain.model.ExportProgress
import com.riox432.civitdeck.domain.repository.ExportRepository
import com.riox432.civitdeck.plugin.ExportFormatPlugin
import com.riox432.civitdeck.plugin.PluginExportFormat
import com.riox432.civitdeck.plugin.PluginExportProgress
import com.riox432.civitdeck.plugin.model.PluginManifest
import com.riox432.civitdeck.plugin.model.PluginState
import com.riox432.civitdeck.plugin.model.PluginType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Adapts the existing ExportRepositoryImpl to the ExportFormatPlugin interface.
 * Provides kohya-ss compatible ZIP export as the built-in default format.
 */
class KohyaZipExportPlugin(
    private val exportRepository: ExportRepository,
) : ExportFormatPlugin {

    override val supportedFormats: List<PluginExportFormat> = listOf(KOHYA_FORMAT)

    override val manifest = PluginManifest(
        id = PLUGIN_ID,
        name = "Kohya-ss ZIP Export",
        version = "1.0.0",
        author = "CivitDeck",
        description = "Export datasets as kohya-ss compatible ZIP archives with captions and manifest",
        pluginType = PluginType.EXPORT_FORMAT,
        capabilities = listOf("DATASET_EXPORT"),
    )

    override var state: PluginState = PluginState.ACTIVE
        private set

    override fun export(
        datasetId: Long,
        formatId: String,
        options: Map<String, String>,
    ): Flow<PluginExportProgress> {
        require(formatId == FORMAT_ID) { "Unsupported format: $formatId" }
        return exportRepository.exportDataset(datasetId, ExportFormat.ZIP)
            .map { it.toPluginProgress() }
    }

    override suspend fun initialize() { /* Built-in — no-op */ }

    override suspend fun activate() {
        state = PluginState.ACTIVE
    }

    override suspend fun deactivate() {
        state = PluginState.INACTIVE
    }

    override suspend fun destroy() {
        state = PluginState.INSTALLED
    }

    companion object {
        const val PLUGIN_ID = "export.kohya-zip"
        const val FORMAT_ID = "kohya-zip"

        private val KOHYA_FORMAT = PluginExportFormat(
            id = FORMAT_ID,
            name = "ZIP (kohya-ss)",
            description = "kohya-ss compatible ZIP with images, captions, and manifest",
            fileExtension = "zip",
            mimeType = "application/zip",
        )
    }
}

/**
 * Maps domain ExportProgress to the plugin-level PluginExportProgress.
 */
internal fun ExportProgress.toPluginProgress(): PluginExportProgress = when (this) {
    is ExportProgress.Preparing -> PluginExportProgress.Preparing
    is ExportProgress.Downloading -> PluginExportProgress.Downloading(current, total)
    is ExportProgress.WritingManifest -> PluginExportProgress.WritingManifest
    is ExportProgress.Completed -> PluginExportProgress.Completed(outputPath, warningCount)
    is ExportProgress.Failed -> PluginExportProgress.Failed(message)
}

/**
 * Maps plugin-level PluginExportProgress back to domain ExportProgress.
 */
internal fun PluginExportProgress.toDomainProgress(): ExportProgress = when (this) {
    is PluginExportProgress.Preparing -> ExportProgress.Preparing
    is PluginExportProgress.Downloading -> ExportProgress.Downloading(current, total)
    is PluginExportProgress.WritingManifest -> ExportProgress.WritingManifest
    is PluginExportProgress.Completed -> ExportProgress.Completed(outputPath, warningCount)
    is PluginExportProgress.Failed -> ExportProgress.Failed(message)
}
