package com.riox432.civitdeck.ui.dataset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.model.ImageSource
import com.riox432.civitdeck.domain.usecase.EditCaptionUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetImagesUseCase
import com.riox432.civitdeck.domain.usecase.RemoveImageFromDatasetUseCase
import com.riox432.civitdeck.domain.usecase.UpdateTrainableUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DatasetDetailViewModel(
    val datasetId: Long,
    observeDatasetImagesUseCase: ObserveDatasetImagesUseCase,
    private val removeImageFromDatasetUseCase: RemoveImageFromDatasetUseCase,
    private val editCaptionUseCase: EditCaptionUseCase,
    private val updateTrainableUseCase: UpdateTrainableUseCase,
) : ViewModel() {

    val images: StateFlow<List<DatasetImage>> =
        observeDatasetImagesUseCase(datasetId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val selectedImageIds = MutableStateFlow<Set<Long>>(emptySet())
    val isSelectionMode = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)
    val selectedSource = MutableStateFlow<ImageSource?>(null)
    val detailImage = MutableStateFlow<DatasetImage?>(null)

    val filteredImages: StateFlow<List<DatasetImage>> = combine(images, selectedSource) { imgs, src ->
        if (src == null) imgs else imgs.filter { it.sourceType == src }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

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
