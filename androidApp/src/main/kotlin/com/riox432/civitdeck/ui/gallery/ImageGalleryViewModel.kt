package com.riox432.civitdeck.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.AspectRatioFilter
import com.riox432.civitdeck.domain.model.Image
import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.usecase.GetImagesUseCase
import com.riox432.civitdeck.domain.usecase.SavePromptUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImageGalleryUiState(
    val allImages: List<Image> = emptyList(),
    val selectedSort: SortOrder = SortOrder.HighestRated,
    val selectedPeriod: TimePeriod = TimePeriod.AllTime,
    val showNsfw: Boolean = false,
    val selectedAspectRatio: AspectRatioFilter? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = true,
    val selectedImageIndex: Int? = null,
) {
    val images: List<Image>
        get() = allImages.filterByAspectRatio(selectedAspectRatio)
}

class ImageGalleryViewModel(
    private val modelVersionId: Long,
    private val getImagesUseCase: GetImagesUseCase,
    private val savePromptUseCase: SavePromptUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageGalleryUiState())
    val uiState: StateFlow<ImageGalleryUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadImages()
    }

    fun onSortSelected(sort: SortOrder) {
        loadJob?.cancel()
        _uiState.update {
            it.copy(
                selectedSort = sort,
                nextCursor = null,
                allImages = emptyList(),
                hasMore = true,
            )
        }
        loadImages()
    }

    fun onPeriodSelected(period: TimePeriod) {
        loadJob?.cancel()
        _uiState.update {
            it.copy(
                selectedPeriod = period,
                nextCursor = null,
                allImages = emptyList(),
                hasMore = true,
            )
        }
        loadImages()
    }

    fun onNsfwToggle() {
        loadJob?.cancel()
        _uiState.update {
            it.copy(
                showNsfw = !it.showNsfw,
                nextCursor = null,
                allImages = emptyList(),
                hasMore = true,
            )
        }
        loadImages()
    }

    fun onAspectRatioSelected(filter: AspectRatioFilter?) {
        _uiState.update { it.copy(selectedAspectRatio = filter) }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        loadImages(isLoadMore = true)
    }

    fun onImageSelected(index: Int) {
        _uiState.update { it.copy(selectedImageIndex = index) }
    }

    fun onDismissViewer() {
        _uiState.update { it.copy(selectedImageIndex = null) }
    }

    fun retry() {
        loadImages()
    }

    fun savePrompt(meta: ImageGenerationMeta, sourceImageUrl: String?) {
        viewModelScope.launch {
            savePromptUseCase(meta, sourceImageUrl)
        }
    }

    private fun loadImages(isLoadMore: Boolean = false) {
        loadJob = viewModelScope.launch {
            _uiState.update {
                if (isLoadMore) {
                    it.copy(isLoadingMore = true)
                } else {
                    it.copy(isLoading = true)
                }
            }
            try {
                val state = _uiState.value
                val nsfwLevel = if (state.showNsfw) NsfwLevel.Soft else NsfwLevel.None
                val result = getImagesUseCase(
                    modelVersionId = modelVersionId,
                    sort = state.selectedSort,
                    period = state.selectedPeriod,
                    nsfwLevel = nsfwLevel,
                    cursor = if (isLoadMore) state.nextCursor else null,
                    limit = PAGE_SIZE,
                )
                _uiState.update {
                    it.copy(
                        allImages = if (isLoadMore) it.allImages + result.items else result.items,
                        isLoading = false,
                        isLoadingMore = false,
                        error = null,
                        nextCursor = result.metadata.nextCursor,
                        hasMore = result.metadata.nextCursor != null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = e.message ?: "Unknown error",
                    )
                }
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}

private fun List<Image>.filterByAspectRatio(filter: AspectRatioFilter?): List<Image> {
    if (filter == null) return this
    return filter { image ->
        if (image.width <= 0 || image.height <= 0) return@filter true
        val ratio = image.width.toFloat() / image.height.toFloat()
        when (filter) {
            AspectRatioFilter.Portrait -> ratio < 0.83f
            AspectRatioFilter.Landscape -> ratio > 1.2f
            AspectRatioFilter.Square -> ratio in 0.83f..1.2f
        }
    }
}
