package com.riox432.civitdeck.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.SetNsfwFilterUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    val selectedBaseModels: Set<BaseModel> = emptySet(),
    val nsfwFilterLevel: NsfwFilterLevel = NsfwFilterLevel.Off,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = true,
)

class ModelSearchViewModel(
    private val getModelsUseCase: GetModelsUseCase,
    private val observeNsfwFilterUseCase: ObserveNsfwFilterUseCase,
    private val setNsfwFilterUseCase: SetNsfwFilterUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelSearchUiState())
    val uiState: StateFlow<ModelSearchUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        observeNsfwFilter()
        loadModels()
    }

    private fun observeNsfwFilter() {
        viewModelScope.launch {
            observeNsfwFilterUseCase().collect { level ->
                _uiState.update { it.copy(nsfwFilterLevel = level) }
            }
        }
    }

    fun onNsfwFilterChanged(level: NsfwFilterLevel) {
        viewModelScope.launch {
            setNsfwFilterUseCase(level)
        }
        loadJob?.cancel()
        _uiState.update {
            it.copy(nextCursor = null, models = emptyList(), hasMore = true)
        }
        loadModels()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onSearch() {
        loadJob?.cancel()
        _uiState.update {
            it.copy(nextCursor = null, models = emptyList(), hasMore = true)
        }
        loadModels()
    }

    fun onTypeSelected(type: ModelType?) {
        loadJob?.cancel()
        _uiState.update {
            it.copy(selectedType = type, nextCursor = null, models = emptyList(), hasMore = true)
        }
        loadModels()
    }

    fun onSortSelected(sort: SortOrder) {
        loadJob?.cancel()
        _uiState.update {
            it.copy(selectedSort = sort, nextCursor = null, models = emptyList(), hasMore = true)
        }
        loadModels()
    }

    fun onPeriodSelected(period: TimePeriod) {
        loadJob?.cancel()
        _uiState.update {
            it.copy(selectedPeriod = period, nextCursor = null, models = emptyList(), hasMore = true)
        }
        loadModels()
    }

    fun onBaseModelToggled(baseModel: BaseModel) {
        loadJob?.cancel()
        _uiState.update {
            val updated = it.selectedBaseModels.toMutableSet()
            if (baseModel in updated) updated.remove(baseModel) else updated.add(baseModel)
            it.copy(selectedBaseModels = updated, nextCursor = null, models = emptyList(), hasMore = true)
        }
        loadModels()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        loadModels(isLoadMore = true)
    }

    fun refresh() {
        loadJob?.cancel()
        _uiState.update { it.copy(isRefreshing = true, nextCursor = null, hasMore = true) }
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
                val result = getModelsUseCase(
                    query = state.query.ifBlank { null },
                    type = state.selectedType,
                    sort = state.selectedSort,
                    period = state.selectedPeriod,
                    baseModels = state.selectedBaseModels.toList().ifEmpty { null },
                    cursor = if (isLoadMore) state.nextCursor else null,
                    limit = PAGE_SIZE,
                )
                val filteredItems = filterNsfw(result.items, state.nsfwFilterLevel)
                _uiState.update {
                    it.copy(
                        models = if (isLoadMore) it.models + filteredItems else filteredItems,
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

    private fun filterNsfw(models: List<Model>, level: NsfwFilterLevel): List<Model> =
        when (level) {
            NsfwFilterLevel.Off -> models.filter { !it.nsfw }
            NsfwFilterLevel.Soft -> models
            NsfwFilterLevel.All -> models
        }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
