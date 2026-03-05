package com.riox432.civitdeck.feature.externalserver.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.feature.externalserver.domain.model.ExternalServerImageFilters
import com.riox432.civitdeck.feature.externalserver.domain.model.ServerImage
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetExternalServerImagesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExternalServerGalleryUiState(
    val images: List<ServerImage> = emptyList(),
    val filters: ExternalServerImageFilters = ExternalServerImageFilters(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
)

private const val PAGE_SIZE = 96

class ExternalServerGalleryViewModel(
    private val getImages: GetExternalServerImagesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExternalServerGalleryUiState())
    val uiState: StateFlow<ExternalServerGalleryUiState> = _uiState.asStateFlow()

    init {
        loadFirstPage()
    }

    fun onFiltersChanged(filters: ExternalServerImageFilters) {
        _uiState.update { it.copy(filters = filters, images = emptyList(), currentPage = 1) }
        loadFirstPage()
    }

    fun onLoadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.currentPage >= state.totalPages) return
        loadPage(state.currentPage + 1)
    }

    fun onRetry() = loadFirstPage()

    private fun loadFirstPage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                getImages(1, PAGE_SIZE, _uiState.value.filters)
            }.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        images = response.images,
                        currentPage = response.page,
                        totalPages = response.totalPages,
                        isLoading = false,
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load images") }
            }
        }
    }

    private fun loadPage(page: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            runCatching {
                getImages(page, PAGE_SIZE, _uiState.value.filters)
            }.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        images = it.images + response.images,
                        currentPage = response.page,
                        totalPages = response.totalPages,
                        isLoadingMore = false,
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoadingMore = false, error = e.message ?: "Failed to load more images") }
            }
        }
    }
}
