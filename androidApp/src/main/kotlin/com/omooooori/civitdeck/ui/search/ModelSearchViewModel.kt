package com.omooooori.civitdeck.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omooooori.civitdeck.domain.model.Model
import com.omooooori.civitdeck.domain.model.ModelType
import com.omooooori.civitdeck.domain.model.SortOrder
import com.omooooori.civitdeck.domain.model.TimePeriod
import com.omooooori.civitdeck.domain.usecase.GetModelsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ModelSearchUiState(
    val models: List<Model> = emptyList(),
    val query: String = "",
    val selectedType: ModelType? = null,
    val selectedSort: SortOrder = SortOrder.MostDownloaded,
    val selectedPeriod: TimePeriod = TimePeriod.AllTime,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
)

class ModelSearchViewModel(
    private val getModelsUseCase: GetModelsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelSearchUiState())
    val uiState: StateFlow<ModelSearchUiState> = _uiState.asStateFlow()

    init {
        loadModels()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onSearch() {
        _uiState.update { it.copy(currentPage = 1, models = emptyList(), hasMore = true) }
        loadModels()
    }

    fun onTypeSelected(type: ModelType?) {
        _uiState.update {
            it.copy(selectedType = type, currentPage = 1, models = emptyList(), hasMore = true)
        }
        loadModels()
    }

    fun onSortSelected(sort: SortOrder) {
        _uiState.update {
            it.copy(selectedSort = sort, currentPage = 1, models = emptyList(), hasMore = true)
        }
        loadModels()
    }

    fun onPeriodSelected(period: TimePeriod) {
        _uiState.update {
            it.copy(selectedPeriod = period, currentPage = 1, models = emptyList(), hasMore = true)
        }
        loadModels()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        _uiState.update { it.copy(currentPage = it.currentPage + 1) }
        loadModels(isLoadMore = true)
    }

    fun refresh() {
        _uiState.update { it.copy(currentPage = 1, models = emptyList(), hasMore = true) }
        loadModels()
    }

    private fun loadModels(isLoadMore: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (isLoadMore) it.copy(isLoadingMore = true) else it.copy(isLoading = true)
            }
            try {
                val state = _uiState.value
                val result = getModelsUseCase(
                    query = state.query.ifBlank { null },
                    type = state.selectedType,
                    sort = state.selectedSort,
                    period = state.selectedPeriod,
                    page = state.currentPage,
                    limit = PAGE_SIZE,
                )
                _uiState.update {
                    it.copy(
                        models = if (isLoadMore) it.models + result.items else result.items,
                        isLoading = false,
                        isLoadingMore = false,
                        error = null,
                        hasMore = result.items.size >= PAGE_SIZE,
                    )
                }
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
