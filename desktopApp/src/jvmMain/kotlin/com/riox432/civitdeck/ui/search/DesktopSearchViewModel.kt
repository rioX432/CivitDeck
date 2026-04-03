package com.riox432.civitdeck.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.ObserveQualityThresholdUseCase
import com.riox432.civitdeck.domain.usecase.QualityScoreCalculator
import com.riox432.civitdeck.domain.util.LoadResult
import com.riox432.civitdeck.domain.util.PaginatedLoader
import com.riox432.civitdeck.feature.search.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.MultiSourceSearchUseCase
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
    val selectedSources: Set<ModelSource> = setOf(ModelSource.CIVITAI),
    val models: List<Model> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = true,
)

class DesktopSearchViewModel(
    private val getModelsUseCase: GetModelsUseCase,
    private val multiSourceSearchUseCase: MultiSourceSearchUseCase,
    private val observeDefaultSortOrderUseCase: ObserveDefaultSortOrderUseCase,
    private val observeDefaultTimePeriodUseCase: ObserveDefaultTimePeriodUseCase,
    private val observeQualityThresholdUseCase: ObserveQualityThresholdUseCase,
) : ViewModel() {

    private var multiSourcePage: Int = 1
    private var qualityThreshold: Int = 0

    private val _uiState = MutableStateFlow(DesktopSearchUiState())
    val uiState: StateFlow<DesktopSearchUiState> = _uiState.asStateFlow()

    private val paginatedLoader = PaginatedLoader<Model>(
        scope = viewModelScope,
        pageSize = PAGE_SIZE,
        load = { cursor, limit -> loadPage(cursor, limit) },
        onStateChanged = { loadState ->
            _uiState.update {
                it.copy(
                    models = loadState.items,
                    isLoading = loadState.isLoading,
                    isLoadingMore = loadState.isLoadingMore,
                    error = loadState.error,
                    nextCursor = loadState.nextCursor,
                    hasMore = loadState.hasMore,
                )
            }
        },
    )

    init {
        viewModelScope.launch {
            val sort = observeDefaultSortOrderUseCase().first()
            val period = observeDefaultTimePeriodUseCase().first()
            _uiState.update { it.copy(selectedSort = sort, selectedPeriod = period) }
            paginatedLoader.loadFirst()
        }
        viewModelScope.launch {
            observeQualityThresholdUseCase().collect { qualityThreshold = it }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onSearch() {
        paginatedLoader.loadFirst()
    }

    fun onTypeSelected(type: ModelType?) {
        _uiState.update { it.copy(selectedType = type) }
        paginatedLoader.loadFirst()
    }

    fun onSortSelected(sort: SortOrder) {
        _uiState.update { it.copy(selectedSort = sort) }
        paginatedLoader.loadFirst()
    }

    fun onPeriodSelected(period: TimePeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        paginatedLoader.loadFirst()
    }

    fun onBaseModelToggled(baseModel: BaseModel) {
        _uiState.update { state ->
            val updated = state.selectedBaseModels.toMutableSet().apply {
                if (baseModel in this) remove(baseModel) else add(baseModel)
            }.toSet()
            state.copy(selectedBaseModels = updated)
        }
        paginatedLoader.loadFirst()
    }

    fun onQualityFilterToggled() {
        _uiState.update { it.copy(isQualityFilterEnabled = !it.isQualityFilterEnabled) }
    }

    fun toggleSource(source: ModelSource) {
        _uiState.update { state ->
            val updated = state.selectedSources.toMutableSet()
            if (source in updated) {
                if (updated.size > 1) updated.remove(source)
            } else {
                updated.add(source)
            }
            state.copy(selectedSources = updated.toSet())
        }
        paginatedLoader.loadFirst()
    }

    fun resetFilters() {
        _uiState.update { DesktopSearchUiState() }
        paginatedLoader.loadFirst()
    }

    fun loadMore() {
        paginatedLoader.loadMore()
    }

    private suspend fun loadPage(cursor: String?, limit: Int): LoadResult<Model> {
        val state = _uiState.value
        val isCivitaiOnly = state.selectedSources == setOf(ModelSource.CIVITAI)
        val raw = if (isCivitaiOnly) {
            loadCivitaiPage(state, cursor, limit)
        } else {
            loadMultiSourcePage(state, cursor, limit)
        }
        val filtered = applyQualityFilter(raw.items, state)
        val sorted = if (state.selectedSort == SortOrder.Quality) {
            filtered.sortedByDescending { QualityScoreCalculator.calculate(it.stats) }
        } else {
            filtered
        }
        return LoadResult(items = sorted, nextCursor = raw.nextCursor)
    }

    private fun applyQualityFilter(models: List<Model>, state: DesktopSearchUiState): List<Model> {
        if (!state.isQualityFilterEnabled || qualityThreshold <= 0) return models
        return models.filter { QualityScoreCalculator.calculate(it.stats) >= qualityThreshold }
    }

    private suspend fun loadCivitaiPage(
        state: DesktopSearchUiState,
        cursor: String?,
        limit: Int,
    ): LoadResult<Model> {
        val result = getModelsUseCase(
            query = state.query.ifBlank { null },
            type = state.selectedType,
            sort = state.selectedSort,
            period = state.selectedPeriod,
            baseModels = state.selectedBaseModels.toList().ifEmpty { null },
            cursor = cursor,
            limit = limit,
        )
        return LoadResult(
            items = result.items,
            nextCursor = result.metadata.nextCursor,
        )
    }

    private suspend fun loadMultiSourcePage(
        state: DesktopSearchUiState,
        cursor: String?,
        limit: Int,
    ): LoadResult<Model> {
        if (cursor == null) multiSourcePage = 1
        val result = multiSourceSearchUseCase(
            query = state.query.ifBlank { null },
            selectedSources = state.selectedSources,
            cursor = cursor,
            page = multiSourcePage,
            limit = limit,
        )
        multiSourcePage++
        return LoadResult(
            items = result.models,
            nextCursor = result.nextCursor,
        )
    }

    companion object {
        private const val PAGE_SIZE = 40
    }
}
