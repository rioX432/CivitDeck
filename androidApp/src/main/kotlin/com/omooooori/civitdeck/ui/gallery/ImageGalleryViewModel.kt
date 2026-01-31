package com.omooooori.civitdeck.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omooooori.civitdeck.domain.model.Image
import com.omooooori.civitdeck.domain.model.NsfwLevel
import com.omooooori.civitdeck.domain.model.SortOrder
import com.omooooori.civitdeck.domain.model.TimePeriod
import com.omooooori.civitdeck.domain.usecase.GetImagesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImageGalleryUiState(
    val images: List<Image> = emptyList(),
    val selectedSort: SortOrder = SortOrder.HighestRated,
    val selectedPeriod: TimePeriod = TimePeriod.AllTime,
    val showNsfw: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = true,
    val selectedImageIndex: Int? = null,
)

class ImageGalleryViewModel(
    private val modelId: Long,
    private val getImagesUseCase: GetImagesUseCase,
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
                images = emptyList(),
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
                images = emptyList(),
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
                images = emptyList(),
                hasMore = true,
            )
        }
        loadImages()
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
                    modelId = modelId,
                    sort = state.selectedSort,
                    period = state.selectedPeriod,
                    nsfwLevel = nsfwLevel,
                    cursor = if (isLoadMore) state.nextCursor else null,
                    limit = PAGE_SIZE,
                )
                _uiState.update {
                    it.copy(
                        images = if (isLoadMore) it.images + result.items else result.items,
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
