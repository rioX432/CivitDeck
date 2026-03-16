package com.riox432.civitdeck.ui.dataset

import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.model.ImageSource
import com.riox432.civitdeck.domain.usecase.EditCaptionUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetImagesUseCase
import com.riox432.civitdeck.domain.usecase.RemoveImageFromDatasetUseCase
import com.riox432.civitdeck.domain.usecase.UpdateTrainableUseCase
import com.riox432.civitdeck.plugin.PluginExportFormat
import com.riox432.civitdeck.usecase.ExportWithPluginUseCase
import com.riox432.civitdeck.usecase.GetAvailableExportFormatsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DesktopDatasetDetailViewModel(
    val datasetId: Long,
    observeDatasetImagesUseCase: ObserveDatasetImagesUseCase,
    private val removeImageFromDatasetUseCase: RemoveImageFromDatasetUseCase,
    private val editCaptionUseCase: EditCaptionUseCase,
    private val updateTrainableUseCase: UpdateTrainableUseCase,
    private val exportWithPluginUseCase: ExportWithPluginUseCase,
    getAvailableExportFormatsUseCase: GetAvailableExportFormatsUseCase,
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val images: StateFlow<List<DatasetImage>> =
        observeDatasetImagesUseCase(datasetId)
            .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val selectedSource = MutableStateFlow<ImageSource?>(null)
    val selectedImageIds = MutableStateFlow<Set<Long>>(emptySet())
    val isSelectionMode = MutableStateFlow(false)

    val filteredImages: StateFlow<List<DatasetImage>> = combine(images, selectedSource) { imgs, src ->
        if (src == null) imgs else imgs.filter { it.sourceType == src }
    }.stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val availableExportFormats: StateFlow<List<PluginExportFormat>> =
        getAvailableExportFormatsUseCase()
            .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    fun setSourceFilter(source: ImageSource?) { selectedSource.value = source }

    fun enterSelectionMode(imageId: Long) {
        isSelectionMode.value = true
        selectedImageIds.value = setOf(imageId)
    }

    fun toggleSelection(imageId: Long) {
        selectedImageIds.value = selectedImageIds.value.let { current ->
            if (imageId in current) current - imageId else current + imageId
        }
        if (selectedImageIds.value.isEmpty()) isSelectionMode.value = false
    }

    fun selectAll() {
        selectedImageIds.value = images.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        selectedImageIds.value = emptySet()
        isSelectionMode.value = false
    }

    fun removeSelected() {
        val ids = selectedImageIds.value.toList()
        if (ids.isEmpty()) return
        scope.launch {
            try {
                removeImageFromDatasetUseCase(ids)
                clearSelection()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Removal failure is non-critical
            }
        }
    }

    fun editCaption(imageId: Long, text: String) {
        scope.launch {
            try {
                editCaptionUseCase(imageId, text)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Caption edit failure is non-critical
            }
        }
    }

    fun updateTrainable(imageId: Long, trainable: Boolean) {
        scope.launch {
            try {
                updateTrainableUseCase(imageId, trainable)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Update failure is non-critical
            }
        }
    }

    fun startExport(formatId: String) {
        scope.launch {
            exportWithPluginUseCase(datasetId, formatId).collect { _ ->
                // Progress handled by export use case
            }
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}

private const val STOP_TIMEOUT = 5_000L
