package com.riox432.civitdeck.usecase

import com.riox432.civitdeck.plugin.ExportFormatPlugin
import com.riox432.civitdeck.plugin.PluginExportFormat
import com.riox432.civitdeck.plugin.PluginRegistry
import com.riox432.civitdeck.plugin.model.PluginState
import com.riox432.civitdeck.plugin.model.PluginType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Retrieves all available export formats from active ExportFormatPlugins.
 */
class GetAvailableExportFormatsUseCase(
    private val pluginRegistry: PluginRegistry,
) {
    /**
     * Returns a flat list of all formats from all active export plugins.
     */
    operator fun invoke(): Flow<List<PluginExportFormat>> =
        pluginRegistry.observePluginsByType(PluginType.EXPORT_FORMAT)
            .map { plugins ->
                plugins.filterIsInstance<ExportFormatPlugin>()
                    .filter { it.state == PluginState.ACTIVE }
                    .flatMap { it.supportedFormats }
            }
}
