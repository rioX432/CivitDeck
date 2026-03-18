package com.riox432.civitdeck.ui.search

import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetModelsUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DesktopSearchUiState(
    val query: String = "",
    val selectedType: ModelType? = null,
    val selectedSort: SortOrder = SortOrder.MostDownloaded,
    val selectedPeriod: TimePeriod = TimePeriod.AllTime,
    val selectedBaseModels: Set<BaseModel> = emptySet(),
    val isQualityFilterEnabled: Boolean = false,
    val models: List<Model> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = true,
)

class DesktopSearchViewModel(
    private val getModelsUseCase: GetModelsUseCase,
    private val observeDefaultSortOrderUseCase: ObserveDefaultSortOrderUseCase,
    private val observeDefaultTimePeriodUseCase: ObserveDefaultTimePeriodUseCase,
) : ViewModel() {

    private val scope = viewModelScope

    private val _uiState = MutableStateFlow(DesktopSearchUiState())
    val uiState: StateFlow<DesktopSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        scope.launch {
            val sort = observeDefaultSortOrderUseCase().first()
            val period = observeDefaultTimePeriodUseCase().first()
            _uiState.update { it.copy(selectedSort = sort, selectedPeriod = period) }
            search()
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onSearch() {
        _uiState.update { it.copy(nextCursor = null, hasMore = true, models = emptyList()) }
        search()
    }

    fun onTypeSelected(type: ModelType?) {
        _uiState.update { it.copy(selectedType = type, nextCursor = null, hasMore = true, models = emptyList()) }
        search()
    }

    fun onSortSelected(sort: SortOrder) {
        _uiState.update { it.copy(selectedSort = sort, nextCursor = null, hasMore = true, models = emptyList()) }
        search()
    }

    fun onPeriodSelected(period: TimePeriod) {
        _uiState.update { it.copy(selectedPeriod = period, nextCursor = null, hasMore = true, models = emptyList()) }
        search()
    }

    fun onBaseModelToggled(baseModel: BaseModel) {
        _uiState.update { state ->
            val updated = state.selectedBaseModels.toMutableSet().apply {
                if (baseModel in this) remove(baseModel) else add(baseModel)
            }.toSet()
            state.copy(selectedBaseModels = updated, nextCursor = null, hasMore = true, models = emptyList())
        }
        search()
    }

    fun onQualityFilterToggled() {
        _uiState.update { it.copy(isQualityFilterEnabled = !it.isQualityFilterEnabled) }
    }

    fun resetFilters() {
        _uiState.update {
            DesktopSearchUiState()
        }
        search()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        search(isLoadMore = true)
    }

    private fun search(isLoadMore: Boolean = false) {
        searchJob?.cancel()
        searchJob = scope.launch {
            _uiState.update {
                if (isLoadMore) it.copy(isLoadingMore = true)
                else it.copy(isLoading = true, error = null)
            }
            try {
                val state = _uiState.value
                val result = getModelsUseCase(
                    query = state.query.ifBlank { null },
                    type = state.selectedType,
                    sort = state.selectedSort,
                    period = state.selectedPeriod,
                    baseModels = state.selectedBaseModels.toList().ifEmpty { null },
                    cursor = if (isLoadMore) state.nextCursor else null,
                    limit = PAGE_SIZE,
                )
                _uiState.update {
                    it.copy(
                        models = if (isLoadMore) it.models + result.items else result.items,
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

    public override fun onCleared() {
        super.onCleared()
    }

    companion object {
        private const val PAGE_SIZE = 40
    }
}
