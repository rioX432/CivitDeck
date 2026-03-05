package com.riox432.civitdeck.ui.dataset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.model.ExportProgress
import com.riox432.civitdeck.domain.model.ImageSource
import com.riox432.civitdeck.domain.usecase.DetectDuplicatesUseCase
import com.riox432.civitdeck.domain.usecase.EditCaptionUseCase
import com.riox432.civitdeck.domain.usecase.ExportDatasetUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetImagesUseCase
import com.riox432.civitdeck.domain.usecase.RemoveImageFromDatasetUseCase
import com.riox432.civitdeck.domain.usecase.UpdateTrainableUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DatasetDetailViewModel(
    val datasetId: Long,
    observeDatasetImagesUseCase: ObserveDatasetImagesUseCase,
    private val removeImageFromDatasetUseCase: RemoveImageFromDatasetUseCase,
    private val editCaptionUseCase: EditCaptionUseCase,
    private val updateTrainableUseCase: UpdateTrainableUseCase,
    detectDuplicatesUseCase: DetectDuplicatesUseCase,
    private val exportDatasetUseCase: ExportDatasetUseCase,
) : ViewModel() {

    val images: StateFlow<List<DatasetImage>> =
        observeDatasetImagesUseCase(datasetId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val selectedImageIds = MutableStateFlow<Set<Long>>(emptySet())
    val isSelectionMode = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)
    val selectedSource = MutableStateFlow<ImageSource?>(null)
    val detailImage = MutableStateFlow<DatasetImage?>(null)
    val showResolutionFilter = MutableStateFlow(false)
    val minWidth = MutableStateFlow(0)
    val minHeight = MutableStateFlow(0)

    val filteredImages: StateFlow<List<DatasetImage>> = combine(images, selectedSource) { imgs, src ->
        if (src == null) imgs else imgs.filter { it.sourceType == src }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val lowResImages: StateFlow<List<DatasetImage>> =
        combine(images, minWidth, minHeight) { imgs, w, h ->
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

    val showExportSheet = MutableStateFlow(false)
    val exportProgress = MutableStateFlow<ExportProgress?>(null)

    fun openExportSheet() { showExportSheet.value = true }
    fun dismissExportSheet() { showExportSheet.value = false }

    fun startExport() {
        showExportSheet.value = false
        viewModelScope.launch {
            exportDatasetUseCase(datasetId).collect { progress ->
                exportProgress.value = progress
            }
        }
    }

    fun dismissExportResult() { exportProgress.value = null }

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
        viewModelScope.launch {
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
        viewModelScope.launch {
            try {
                editCaptionUseCase(imageId, text)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Caption edit failure is non-critical
            }
        }
    }

    fun setSourceFilter(source: ImageSource?) { selectedSource.value = source }

    fun showDetail(image: DatasetImage) { detailImage.value = image }

    fun dismissDetail() { detailImage.value = null }

    fun setResolutionFilter(w: Int, h: Int) {
        minWidth.value = w
        minHeight.value = h
    }

    fun openResolutionFilter() { showResolutionFilter.value = true }

    fun dismissResolutionFilter() { showResolutionFilter.value = false }

    fun updateTrainable(imageId: Long, trainable: Boolean) {
        viewModelScope.launch {
            try {
                updateTrainableUseCase(imageId, trainable)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Trainable update failure is non-critical
            }
        }
    }
}

private const val STOP_TIMEOUT = 5_000L
