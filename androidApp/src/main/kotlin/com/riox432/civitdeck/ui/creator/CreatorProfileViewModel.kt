package com.riox432.civitdeck.ui.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.usecase.GetCreatorModelsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreatorProfileUiState(
    val username: String = "",
    val models: List<Model> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = true,
)

class CreatorProfileViewModel(
    private val username: String,
    private val getCreatorModelsUseCase: GetCreatorModelsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatorProfileUiState(username = username))
    val uiState: StateFlow<CreatorProfileUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadModels()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        loadModels(isLoadMore = true)
    }

    fun refresh() {
        loadJob?.cancel()
        _uiState.update { it.copy(nextCursor = null, hasMore = true) }
        loadModels(isRefresh = true)
    }

    private fun loadModels(isLoadMore: Boolean = false, isRefresh: Boolean = false) {
        loadJob = viewModelScope.launch {
            _uiState.update {
                when {
                    isLoadMore -> it.copy(isLoadingMore = true)
                    isRefresh -> it.copy(isRefreshing = true)
                    else -> it.copy(isLoading = true)
                }
            }
            try {
                val state = _uiState.value
                val result = getCreatorModelsUseCase(
                    username = username,
                    cursor = if (isLoadMore) state.nextCursor else null,
                    limit = PAGE_SIZE,
                )
                _uiState.update {
                    it.copy(
                        models = if (isLoadMore) it.models + result.items else result.items,
                        isLoading = false,
                        isLoadingMore = false,
                        isRefreshing = false,
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
                        isRefreshing = false,
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
