package com.riox432.civitdeck.presentation.dataset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.model.ExportProgress
import com.riox432.civitdeck.domain.model.ImageSource
import com.riox432.civitdeck.domain.usecase.DetectDuplicatesUseCase
import com.riox432.civitdeck.domain.usecase.EditCaptionUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetImagesUseCase
import com.riox432.civitdeck.domain.usecase.RemoveImageFromDatasetUseCase
import com.riox432.civitdeck.domain.usecase.UpdateTrainableUseCase
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.plugin.PluginExportFormat
import com.riox432.civitdeck.usecase.ExportWithPluginUseCase
import com.riox432.civitdeck.usecase.GetAvailableExportFormatsUseCase
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Suppress("LongParameterList", "TooManyFunctions")
class DatasetDetailViewModel(
    val datasetId: Long,
    observeDatasetImagesUseCase: ObserveDatasetImagesUseCase,
    private val removeImageFromDatasetUseCase: RemoveImageFromDatasetUseCase,
    private val editCaptionUseCase: EditCaptionUseCase,
    private val updateTrainableUseCase: UpdateTrainableUseCase,
    detectDuplicatesUseCase: DetectDuplicatesUseCase,
    private val exportWithPluginUseCase: ExportWithPluginUseCase,
    getAvailableExportFormatsUseCase: GetAvailableExportFormatsUseCase,
) : ViewModel() {

    val images: StateFlow<List<DatasetImage>> =
        observeDatasetImagesUseCase(datasetId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    private val _selectedImageIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedImageIds: StateFlow<Set<Long>> = _selectedImageIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedSource = MutableStateFlow<ImageSource?>(null)
    val selectedSource: StateFlow<ImageSource?> = _selectedSource.asStateFlow()

    private val _detailImage = MutableStateFlow<DatasetImage?>(null)
    val detailImage: StateFlow<DatasetImage?> = _detailImage.asStateFlow()

    private val _showResolutionFilter = MutableStateFlow(false)
    val showResolutionFilter: StateFlow<Boolean> = _showResolutionFilter.asStateFlow()

    private val _minWidth = MutableStateFlow(0)
    val minWidth: StateFlow<Int> = _minWidth.asStateFlow()

    private val _minHeight = MutableStateFlow(0)
    val minHeight: StateFlow<Int> = _minHeight.asStateFlow()

    val filteredImages: StateFlow<List<DatasetImage>> =
        combine(images, _selectedSource) { imgs, src ->
            if (src == null) imgs else imgs.filter { it.sourceType == src }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val lowResImages: StateFlow<List<DatasetImage>> =
        combine(images, _minWidth, _minHeight) { imgs, w, h ->
            if (w == 0 && h == 0) {
                emptyList()
            } else {
                imgs.filter { img ->
                    val imgW = img.width
                    val imgH = img.height
                    imgW != null && imgH != null && (imgW < w || imgH < h)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val duplicateCount: StateFlow<Int> =
        detectDuplicatesUseCase(datasetId)
            .map { groups -> groups.sumOf { it.images.size } - groups.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), 0)

    private val _showExportSheet = MutableStateFlow(false)
    val showExportSheet: StateFlow<Boolean> = _showExportSheet.asStateFlow()

    private val _exportProgress = MutableStateFlow<ExportProgress?>(null)
    val exportProgress: StateFlow<ExportProgress?> = _exportProgress.asStateFlow()

    private val _selectedExportFormatId = MutableStateFlow<String?>(null)
    val selectedExportFormatId: StateFlow<String?> = _selectedExportFormatId.asStateFlow()

    val availableExportFormats: StateFlow<List<PluginExportFormat>> =
        getAvailableExportFormatsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    fun openExportSheet() { _showExportSheet.value = true }
    fun dismissExportSheet() { _showExportSheet.value = false }

    fun selectExportFormat(formatId: String) { _selectedExportFormatId.value = formatId }

    fun startExport(formatId: String) {
        _showExportSheet.value = false
        viewModelScope.launch {
            exportWithPluginUseCase(datasetId, formatId).collect { progress ->
                _exportProgress.value = progress
            }
        }
    }

    fun dismissExportResult() { _exportProgress.value = null }

    fun enterSelectionMode(imageId: Long) {
        _isSelectionMode.value = true
        _selectedImageIds.value = setOf(imageId)
    }

    fun toggleSelection(imageId: Long) {
        _selectedImageIds.value = _selectedImageIds.value.let { current ->
            if (imageId in current) current - imageId else current + imageId
        }
        if (_selectedImageIds.value.isEmpty()) _isSelectionMode.value = false
    }

    fun selectAll() {
        _selectedImageIds.value = images.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedImageIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun removeSelected() {
        val ids = _selectedImageIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            suspendRunCatching { removeImageFromDatasetUseCase(ids) }
                .onSuccess { clearSelection() }
                .onFailure { e -> Logger.w(TAG, "Remove images failed: ${e.message}") }
        }
    }

    fun editCaption(imageId: Long, text: String) {
        viewModelScope.launch {
            suspendRunCatching { editCaptionUseCase(imageId, text) }
                .onFailure { e -> Logger.w(TAG, "Edit caption failed: ${e.message}") }
        }
    }

    fun setSourceFilter(source: ImageSource?) { _selectedSource.value = source }

    fun showDetail(image: DatasetImage) { _detailImage.value = image }

    fun dismissDetail() { _detailImage.value = null }

    fun setResolutionFilter(w: Int, h: Int) {
        _minWidth.value = w
        _minHeight.value = h
    }

    fun openResolutionFilter() { _showResolutionFilter.value = true }

    fun dismissResolutionFilter() { _showResolutionFilter.value = false }

    fun updateTrainable(imageId: Long, trainable: Boolean) {
        viewModelScope.launch {
            suspendRunCatching { updateTrainableUseCase(imageId, trainable) }
                .onFailure { e -> Logger.w(TAG, "Update trainable failed: ${e.message}") }
        }
    }
}

private const val TAG = "DatasetDetailViewModel"
private const val STOP_TIMEOUT = 5_000L
