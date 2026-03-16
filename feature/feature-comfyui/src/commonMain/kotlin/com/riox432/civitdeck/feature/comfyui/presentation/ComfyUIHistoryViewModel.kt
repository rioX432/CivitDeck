package com.riox432.civitdeck.feature.comfyui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.data.image.SaveGeneratedImageUseCase
import com.riox432.civitdeck.domain.model.ComfyUIGeneratedImage
import com.riox432.civitdeck.domain.model.DatasetCollection
import com.riox432.civitdeck.domain.model.ImageSource
import com.riox432.civitdeck.domain.model.ShareHashtag
import com.riox432.civitdeck.domain.usecase.AddImageToDatasetUseCase
import com.riox432.civitdeck.domain.usecase.AddShareHashtagUseCase
import com.riox432.civitdeck.domain.usecase.CreateDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveShareHashtagsUseCase
import com.riox432.civitdeck.domain.usecase.RemoveShareHashtagUseCase
import com.riox432.civitdeck.domain.usecase.ToggleShareHashtagUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUIHistoryUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT = 5_000L

enum class HistorySortOrder { Newest, Oldest }

data class ComfyUIHistoryUiState(
    val images: List<ComfyUIGeneratedImage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedSort: HistorySortOrder = HistorySortOrder.Newest,
    val imageSaveSuccess: Boolean? = null,
    val showDatasetPicker: Boolean = false,
    val pendingImageForDataset: ComfyUIGeneratedImage? = null,
    val addToDatasetSuccess: Boolean? = null,
)

class ComfyUIHistoryViewModel(
    private val fetchHistory: FetchComfyUIHistoryUseCase,
    private val saveImage: SaveGeneratedImageUseCase,
    observeDatasetCollections: ObserveDatasetCollectionsUseCase,
    private val addImageToDataset: AddImageToDatasetUseCase,
    private val createDatasetCollection: CreateDatasetCollectionUseCase,
    observeShareHashtags: ObserveShareHashtagsUseCase,
    private val addShareHashtag: AddShareHashtagUseCase,
    private val removeShareHashtag: RemoveShareHashtagUseCase,
    private val toggleShareHashtag: ToggleShareHashtagUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComfyUIHistoryUiState())
    val uiState: StateFlow<ComfyUIHistoryUiState> = _uiState

    val datasets: StateFlow<List<DatasetCollection>> =
        observeDatasetCollections()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val shareHashtags: StateFlow<List<ShareHashtag>> =
        observeShareHashtags()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            fetchHistory()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: e.toString()) }
                }
                .collect { images ->
                    _uiState.update {
                        it.copy(isLoading = false, images = images)
                    }
                }
        }
    }

    fun onSelectSort(sort: HistorySortOrder) {
        _uiState.update { it.copy(selectedSort = sort) }
    }

    fun onSaveImage(imageUrl: String, filename: String) {
        viewModelScope.launch {
            val success = saveImage(imageUrl, filename)
            _uiState.update { it.copy(imageSaveSuccess = success) }
        }
    }

    fun onDismissSaveResult() {
        _uiState.update { it.copy(imageSaveSuccess = null) }
    }

    fun filteredImages(): List<ComfyUIGeneratedImage> {
        val state = _uiState.value
        return when (state.selectedSort) {
            HistorySortOrder.Newest -> state.images.reversed()
            HistorySortOrder.Oldest -> state.images
        }
    }

    fun onAddToDatasetTap(image: ComfyUIGeneratedImage) {
        _uiState.update { it.copy(pendingImageForDataset = image, showDatasetPicker = true) }
    }

    fun onDatasetSelected(datasetId: Long) {
        val image = _uiState.value.pendingImageForDataset ?: return
        _uiState.update { it.copy(showDatasetPicker = false, pendingImageForDataset = null) }
        viewModelScope.launch {
            try {
                val tags = buildDatasetTags(image)
                addImageToDataset(
                    datasetId = datasetId,
                    imageUrl = image.imageUrl,
                    sourceType = ImageSource.GENERATED,
                    trainable = true,
                    tags = tags,
                )
                _uiState.update { it.copy(addToDatasetSuccess = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(addToDatasetSuccess = false) }
            }
        }
    }

    fun onCreateDatasetAndSelect(name: String) {
        val image = _uiState.value.pendingImageForDataset ?: return
        _uiState.update { it.copy(showDatasetPicker = false, pendingImageForDataset = null) }
        viewModelScope.launch {
            try {
                val datasetId = createDatasetCollection(name)
                val tags = buildDatasetTags(image)
                addImageToDataset(
                    datasetId = datasetId,
                    imageUrl = image.imageUrl,
                    sourceType = ImageSource.GENERATED,
                    trainable = true,
                    tags = tags,
                )
                _uiState.update { it.copy(addToDatasetSuccess = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(addToDatasetSuccess = false) }
            }
        }
    }

    fun onDismissDatasetPicker() {
        _uiState.update { it.copy(showDatasetPicker = false, pendingImageForDataset = null) }
    }

    fun onDismissDatasetResult() {
        _uiState.update { it.copy(addToDatasetSuccess = null) }
    }

    fun onToggleShareHashtag(tag: String, isEnabled: Boolean) {
        viewModelScope.launch { toggleShareHashtag(tag, isEnabled) }
    }

    fun onAddShareHashtag(tag: String) {
        viewModelScope.launch { addShareHashtag(tag) }
    }

    fun onRemoveShareHashtag(tag: String) {
        viewModelScope.launch { removeShareHashtag(tag) }
    }

    private fun buildDatasetTags(image: ComfyUIGeneratedImage): List<String> {
        val tags = mutableListOf<String>()
        image.meta.seed?.let { tags.add("seed:$it") }
        image.meta.samplerName?.let { tags.add("sampler:$it") }
        if (image.meta.positivePrompt.isNotBlank()) {
            tags.add("prompt_hash:${image.meta.positivePrompt.hashCode()}")
        }
        return tags
    }
}
