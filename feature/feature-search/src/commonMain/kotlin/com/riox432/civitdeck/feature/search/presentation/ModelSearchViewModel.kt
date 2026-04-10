package com.riox432.civitdeck.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.RecommendationSection
import com.riox432.civitdeck.domain.model.SavedSearchFilter
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.usecase.AddExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.GetExcludedTagsUseCase
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.ObserveFavoritesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObserveOwnedModelHashesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveQualityThresholdUseCase
import com.riox432.civitdeck.domain.usecase.RemoveExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.util.PaginatedLoader
import com.riox432.civitdeck.domain.util.UiLoadingState
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.feature.search.domain.usecase.AddSearchHistoryUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.DeleteSavedSearchFilterUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.DeleteSearchHistoryItemUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetHiddenModelIdsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetRecommendationsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.HideModelUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.MultiSourceSearchUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.ObserveSavedSearchFiltersUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.ObserveSearchHistoryUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.SaveSearchFilterUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.TrackRecommendationClickUseCase
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ModelSearchUiState(
    val query: String = "",
    val selectedType: ModelType? = null,
    val selectedSort: SortOrder = SortOrder.MostDownloaded,
    val selectedPeriod: TimePeriod = TimePeriod.AllTime,
    val selectedBaseModels: Set<BaseModel> = emptySet(),
    val nsfwFilterLevel: NsfwFilterLevel = NsfwFilterLevel.Off,
    val isFreshFindEnabled: Boolean = false,
    val isQualityFilterEnabled: Boolean = false,
    val recommendations: List<RecommendationSection> = emptyList(),
    val isLoadingRecommendations: Boolean = false,
    val excludedTags: List<String> = emptyList(),
    val includedTags: List<String> = emptyList(),
    val selectedSources: Set<ModelSource> = setOf(ModelSource.CIVITAI),
    // Pagination state from PaginatedLoader
    val models: List<Model> = emptyList(),
    override val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    override val error: String? = null,
    val hasMore: Boolean = true,
) : UiLoadingState

/**
 * Internal filter state used to track all filter parameters and trigger reloads.
 */
internal data class FilterState(
    val query: String = "",
    val selectedType: ModelType? = null,
    val selectedSort: SortOrder = SortOrder.MostDownloaded,
    val selectedPeriod: TimePeriod = TimePeriod.AllTime,
    val selectedBaseModels: Set<BaseModel> = emptySet(),
    val nsfwFilterLevel: NsfwFilterLevel = NsfwFilterLevel.Off,
    val isFreshFindEnabled: Boolean = false,
    val isQualityFilterEnabled: Boolean = false,
    val qualityThreshold: Int = 0,
    val excludedTags: List<String> = emptyList(),
    val includedTags: List<String> = emptyList(),
    val selectedSources: Set<ModelSource> = setOf(ModelSource.CIVITAI),
)

@Suppress("LongParameterList")
class ModelSearchViewModel(
    private val getModelsUseCase: GetModelsUseCase,
    private val multiSourceSearchUseCase: MultiSourceSearchUseCase,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val observeNsfwFilterUseCase: ObserveNsfwFilterUseCase,
    observeSearchHistoryUseCase: ObserveSearchHistoryUseCase,
    private val addSearchHistoryUseCase: AddSearchHistoryUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase,
    private val deleteSearchHistoryItemUseCase: DeleteSearchHistoryItemUseCase,
    private val getViewedModelIdsUseCase: GetViewedModelIdsUseCase,
    private val getExcludedTagsUseCase: GetExcludedTagsUseCase,
    private val addExcludedTagUseCase: AddExcludedTagUseCase,
    private val removeExcludedTagUseCase: RemoveExcludedTagUseCase,
    private val getHiddenModelIdsUseCase: GetHiddenModelIdsUseCase,
    private val hideModelUseCase: HideModelUseCase,
    observeGridColumnsUseCase: ObserveGridColumnsUseCase,
    observeDefaultSortOrderUseCase: ObserveDefaultSortOrderUseCase,
    observeDefaultTimePeriodUseCase: ObserveDefaultTimePeriodUseCase,
    observeOwnedModelHashesUseCase: ObserveOwnedModelHashesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    observeFavoritesUseCase: ObserveFavoritesUseCase,
    observeSavedSearchFiltersUseCase: ObserveSavedSearchFiltersUseCase,
    private val saveSearchFilterUseCase: SaveSearchFilterUseCase,
    private val deleteSavedSearchFilterUseCase: DeleteSavedSearchFilterUseCase,
    private val observeQualityThresholdUseCase: ObserveQualityThresholdUseCase,
    private val trackRecommendationClickUseCase: TrackRecommendationClickUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelSearchUiState())
    val uiState: StateFlow<ModelSearchUiState> = _uiState.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    private val _hiddenModelIds = MutableStateFlow<Set<Long>>(emptySet())
    private var recommendationsJob: Job? = null

    private val pageLoader = SearchPageLoader(
        getModelsUseCase = getModelsUseCase,
        multiSourceSearchUseCase = multiSourceSearchUseCase,
        getViewedModelIdsUseCase = getViewedModelIdsUseCase,
        hiddenModelIds = _hiddenModelIds,
    )

    private val filterDelegate = SearchFilterDelegate(
        scope = viewModelScope,
        filterState = _filterState,
        hiddenModelIds = _hiddenModelIds,
        getExcludedTagsUseCase = getExcludedTagsUseCase,
        addExcludedTagUseCase = addExcludedTagUseCase,
        removeExcludedTagUseCase = removeExcludedTagUseCase,
        getHiddenModelIdsUseCase = getHiddenModelIdsUseCase,
        hideModelUseCase = hideModelUseCase,
        saveSearchFilterUseCase = saveSearchFilterUseCase,
        deleteSavedSearchFilterUseCase = deleteSavedSearchFilterUseCase,
        updateFilter = { transform -> updateFilter(transform) },
        resetPaginationAndReload = ::refresh,
    )

    val searchHistory: StateFlow<List<String>> =
        observeSearchHistoryUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val gridColumns: StateFlow<Int> =
        observeGridColumnsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), 2)

    val ownedHashes: StateFlow<Set<String>> =
        observeOwnedModelHashesUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptySet())

    val favoriteIds: StateFlow<Set<Long>> =
        observeFavoritesUseCase()
            .map { favorites -> favorites.map { it.id }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptySet())

    val savedFilters: StateFlow<List<SavedSearchFilter>> =
        observeSavedSearchFiltersUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    private val paginatedLoader = PaginatedLoader<Model>(
        scope = viewModelScope,
        pageSize = SearchPageLoader.PAGE_SIZE,
        load = { cursor, limit -> pageLoader.loadPage(_filterState.value, cursor, limit) },
        onStateChanged = { loadState ->
            _uiState.update {
                it.copy(
                    models = loadState.items,
                    isLoading = loadState.isLoading,
                    isLoadingMore = loadState.isLoadingMore,
                    error = loadState.error,
                    hasMore = loadState.hasMore,
                )
            }
        },
    )

    init {
        observeNsfwFilter()
        observeQualityThreshold()
        filterDelegate.loadExcludedTags()
        loadDefaults(observeDefaultSortOrderUseCase, observeDefaultTimePeriodUseCase)
        loadRecommendations()
    }

    // region Filter actions

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onSearch() {
        val query = _uiState.value.query
        if (query.isNotBlank()) {
            viewModelScope.launch { addSearchHistoryUseCase(query.trim()) }
        }
        _filterState.update { it.copy(query = query) }
        refresh()
    }

    fun onHistoryItemClick(query: String) {
        _uiState.update { it.copy(query = query) }
        onSearch()
    }

    fun removeSearchHistoryItem(query: String) {
        viewModelScope.launch { deleteSearchHistoryItemUseCase(query) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { clearSearchHistoryUseCase() }
    }

    fun resetFilters() {
        updateFilter {
            it.copy(
                selectedType = null,
                selectedSort = SortOrder.MostDownloaded,
                selectedPeriod = TimePeriod.AllTime,
                selectedBaseModels = emptySet(),
                isFreshFindEnabled = false,
                isQualityFilterEnabled = false,
                includedTags = emptyList(),
                selectedSources = setOf(ModelSource.CIVITAI),
            )
        }
        refresh()
    }

    fun onTypeSelected(type: ModelType?) {
        updateFilter { it.copy(selectedType = type) }
        refresh()
    }

    fun onSortSelected(sort: SortOrder) {
        updateFilter { it.copy(selectedSort = sort) }
        refresh()
    }

    fun onPeriodSelected(period: TimePeriod) {
        updateFilter { it.copy(selectedPeriod = period) }
        refresh()
    }

    fun onBaseModelToggled(baseModel: BaseModel) {
        updateFilter {
            val updated = it.selectedBaseModels.toMutableSet().apply {
                if (baseModel in this) remove(baseModel) else add(baseModel)
            }.toSet()
            it.copy(selectedBaseModels = updated)
        }
        refresh()
    }

    fun onFreshFindToggled() {
        updateFilter { it.copy(isFreshFindEnabled = !it.isFreshFindEnabled) }
        refresh()
    }

    fun onQualityFilterToggled() {
        updateFilter { it.copy(isQualityFilterEnabled = !it.isQualityFilterEnabled) }
        refresh()
    }

    fun onAddIncludedTag(tag: String) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isBlank()) return
        val current = _filterState.value.includedTags
        if (trimmed in current) return
        updateFilter { it.copy(includedTags = it.includedTags + trimmed) }
        refresh()
    }

    fun onRemoveIncludedTag(tag: String) {
        updateFilter { it.copy(includedTags = it.includedTags - tag) }
        refresh()
    }

    fun toggleSource(source: ModelSource) {
        updateFilter {
            val updated = it.selectedSources.toMutableSet()
            if (source in updated) {
                if (updated.size > 1) updated.remove(source)
            } else {
                updated.add(source)
            }
            it.copy(selectedSources = updated.toSet())
        }
        refresh()
    }

    // endregion

    // region Excluded tags, hidden models & saved filters (delegated)

    fun onAddExcludedTag(tag: String) = filterDelegate.onAddExcludedTag(tag)
    fun onRemoveExcludedTag(tag: String) = filterDelegate.onRemoveExcludedTag(tag)
    fun onHideModel(modelId: Long, modelName: String) = filterDelegate.onHideModel(modelId, modelName)
    fun saveCurrentFilter(name: String) = filterDelegate.saveCurrentFilter(name)
    fun applyFilter(filter: SavedSearchFilter) = filterDelegate.applyFilter(filter)
    fun deleteSavedFilter(id: Long) = filterDelegate.deleteSavedFilter(id)

    // endregion

    // region Favorites & recommendations

    fun trackRecommendationClick(modelId: Long) {
        viewModelScope.launch {
            suspendRunCatching { trackRecommendationClickUseCase(modelId) }
        }
    }

    fun toggleFavorite(model: Model) {
        viewModelScope.launch {
            suspendRunCatching { toggleFavoriteUseCase(model) }
        }
    }

    // endregion

    // region Pagination

    fun loadMore() {
        paginatedLoader.loadMore()
    }

    fun refresh() {
        pageLoader.resetPagination()
        paginatedLoader.loadFirst()
    }

    // endregion

    // region Private helpers

    private fun loadDefaults(
        observeSortUseCase: ObserveDefaultSortOrderUseCase,
        observePeriodUseCase: ObserveDefaultTimePeriodUseCase,
    ) {
        viewModelScope.launch {
            val sort = observeSortUseCase().first()
            val period = observePeriodUseCase().first()
            _uiState.update { it.copy(selectedSort = sort, selectedPeriod = period) }
            val current = _filterState.value
            if (current.selectedSort != sort || current.selectedPeriod != period) {
                _filterState.update { it.copy(selectedSort = sort, selectedPeriod = period) }
            }
            // Initial load after defaults are set
            paginatedLoader.loadFirst()
        }
    }

    private fun observeNsfwFilter() {
        viewModelScope.launch {
            observeNsfwFilterUseCase().collect { level ->
                val prev = _uiState.value.nsfwFilterLevel
                _uiState.update { it.copy(nsfwFilterLevel = level) }
                if (prev != level) {
                    _filterState.update { it.copy(nsfwFilterLevel = level) }
                    loadRecommendations()
                    refresh()
                }
            }
        }
    }

    private fun observeQualityThreshold() {
        viewModelScope.launch {
            observeQualityThresholdUseCase().collect { threshold ->
                _filterState.update { it.copy(qualityThreshold = threshold) }
            }
        }
    }

    private fun loadRecommendations() {
        recommendationsJob?.cancel()
        recommendationsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRecommendations = true) }
            try {
                val sections = getRecommendationsUseCase()
                _uiState.update {
                    it.copy(recommendations = sections, isLoadingRecommendations = false)
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w(TAG, "Failed to load recommendations: ${e.message}")
                _uiState.update { it.copy(isLoadingRecommendations = false) }
            }
        }
    }

    internal val updateFilter: (transform: (FilterState) -> FilterState) -> Unit = { transform ->
        _filterState.update(transform)
        val f = _filterState.value
        _uiState.update {
            it.copy(
                query = f.query,
                selectedType = f.selectedType,
                selectedSort = f.selectedSort,
                selectedPeriod = f.selectedPeriod,
                selectedBaseModels = f.selectedBaseModels,
                nsfwFilterLevel = f.nsfwFilterLevel,
                isFreshFindEnabled = f.isFreshFindEnabled,
                isQualityFilterEnabled = f.isQualityFilterEnabled,
                excludedTags = f.excludedTags,
                includedTags = f.includedTags,
                selectedSources = f.selectedSources,
            )
        }
    }

    // endregion

    companion object {
        private const val TAG = "ModelSearchViewModel"
        private const val STOP_TIMEOUT = 5_000L
    }
}
