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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.util.Logger
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
) : ViewModel() {

    private val scope = viewModelScope

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
            suspendRunCatching { removeImageFromDatasetUseCase(ids) }
                .onSuccess { clearSelection() }
                .onFailure { e -> Logger.w(TAG, "Remove images failed: ${e.message}") }
        }
    }

    fun editCaption(imageId: Long, text: String) {
        scope.launch {
            suspendRunCatching { editCaptionUseCase(imageId, text) }
                .onFailure { e -> Logger.w(TAG, "Edit caption failed: ${e.message}") }
        }
    }

    fun updateTrainable(imageId: Long, trainable: Boolean) {
        scope.launch {
            suspendRunCatching { updateTrainableUseCase(imageId, trainable) }
                .onFailure { e -> Logger.w(TAG, "Update trainable failed: ${e.message}") }
        }
    }

    fun startExport(formatId: String) {
        scope.launch {
            exportWithPluginUseCase(datasetId, formatId).collect { _ ->
                // Progress handled by export use case
            }
        }
    }

    public override fun onCleared() {
        super.onCleared()
    }
}

private const val TAG = "DesktopDatasetDetailViewModel"
private const val STOP_TIMEOUT = 5_000L
