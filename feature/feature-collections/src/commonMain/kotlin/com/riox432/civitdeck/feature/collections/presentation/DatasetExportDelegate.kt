package com.riox432.civitdeck.feature.collections.presentation

import com.riox432.civitdeck.domain.model.ExportProgress
import com.riox432.civitdeck.feature.collections.domain.usecase.ExportWithPluginUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.GetAvailableExportFormatsUseCase
import com.riox432.civitdeck.plugin.PluginExportFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class DatasetExportDelegate(
    private val datasetId: Long,
    private val scope: CoroutineScope,
    private val exportWithPluginUseCase: ExportWithPluginUseCase,
    getAvailableExportFormatsUseCase: GetAvailableExportFormatsUseCase,
) {

    private val _showExportSheet = MutableStateFlow(false)
    val showExportSheet: StateFlow<Boolean> = _showExportSheet.asStateFlow()

    private val _exportProgress = MutableStateFlow<ExportProgress?>(null)
    val exportProgress: StateFlow<ExportProgress?> = _exportProgress.asStateFlow()

    private val _selectedExportFormatId = MutableStateFlow<String?>(null)
    val selectedExportFormatId: StateFlow<String?> = _selectedExportFormatId.asStateFlow()

    val availableExportFormats: StateFlow<List<PluginExportFormat>> =
        getAvailableExportFormatsUseCase()
            .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    fun openExportSheet() { _showExportSheet.value = true }
    fun dismissExportSheet() { _showExportSheet.value = false }

    fun selectExportFormat(formatId: String) { _selectedExportFormatId.value = formatId }

    fun startExport(formatId: String) {
        _showExportSheet.value = false
        scope.launch {
            exportWithPluginUseCase(datasetId, formatId).collect { progress ->
                _exportProgress.value = progress
            }
        }
    }

    fun dismissExportResult() { _exportProgress.value = null }
}

private const val STOP_TIMEOUT = 5_000L
