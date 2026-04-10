package com.riox432.civitdeck.feature.collections.domain.usecase

import com.riox432.civitdeck.domain.model.ExportProgress
import com.riox432.civitdeck.plugin.ExportFormatPlugin
import com.riox432.civitdeck.plugin.PluginExportProgress
import com.riox432.civitdeck.plugin.PluginRegistry
import com.riox432.civitdeck.plugin.model.PluginState
import com.riox432.civitdeck.plugin.model.PluginType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Exports a dataset using the selected export format plugin.
 * Falls back to an error if the format or plugin is not found.
 */
class ExportWithPluginUseCase(
    private val pluginRegistry: PluginRegistry,
) {
    /**
     * @param datasetId the dataset collection to export
     * @param formatId the format ID to use (e.g. "kohya-zip")
     * @param options optional key-value parameters for the export
     * @return a flow of domain [ExportProgress] events
     */
    operator fun invoke(
        datasetId: Long,
        formatId: String,
        options: Map<String, String> = emptyMap(),
    ): Flow<ExportProgress> {
        val plugin = findPluginForFormat(formatId)
            ?: return flowOf(ExportProgress.Failed("No export plugin found for format: $formatId"))

        return plugin.export(datasetId, formatId, options)
            .map { it.toDomainProgress() }
    }

    private fun findPluginForFormat(formatId: String): ExportFormatPlugin? =
        pluginRegistry.getPluginsByType(PluginType.EXPORT_FORMAT)
            .filterIsInstance<ExportFormatPlugin>()
            .filter { it.state == PluginState.ACTIVE }
            .firstOrNull { plugin -> plugin.supportedFormats.any { it.id == formatId } }
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
