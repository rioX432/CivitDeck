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
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.util.PaginatedLoader
import com.riox432.civitdeck.domain.util.UiLoadingState
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
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
    val loadedRecommendations: List<RecommendationSection> = emptyList(),
    val isLoadingRecommendations: Boolean = false,
    val hasActiveSearch: Boolean = false,
    val excludedTags: List<String> = emptyList(),
    val includedTags: List<String> = emptyList(),
    val selectedSources: Set<ModelSource> = setOf(ModelSource.CIVITAI),
    // Pagination state from PaginatedLoader
    val models: List<Model> = emptyList(),
    override val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    override val error: String? = null,
    val hasMore: Boolean = true,
) : UiLoadingState {

    /**
     * Recommendation sections the UI should render. Hidden entirely while a search
     * query or a narrowing filter is active (search results must come first), and
     * capped while browsing idle so the model grid stays above the fold.
     */
    val recommendations: List<RecommendationSection>
        get() = if (hasActiveSearch) emptyList() else loadedRecommendations.take(MAX_IDLE_RECOMMENDATION_SECTIONS)

    companion object {
        const val MAX_IDLE_RECOMMENDATION_SECTIONS = 2
    }
}

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

/** True when the user is actively narrowing results rather than browsing idle. */
internal fun FilterState.hasActiveSearch(): Boolean =
    query.isNotBlank() ||
        selectedType != null ||
        selectedBaseModels.isNotEmpty() ||
        includedTags.isNotEmpty() ||
        isFreshFindEnabled

class ModelSearchViewModel(
    private val coreUseCases: SearchCoreUseCases,
    private val historyUseCases: SearchHistoryUseCases,
    private val filterUseCases: SearchFilterUseCases,
    private val preferencesUseCases: SearchPreferencesUseCases,
    private val favoritesUseCases: SearchFavoritesUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelSearchUiState())
    val uiState: StateFlow<ModelSearchUiState> = _uiState.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    private val _hiddenModelIds = MutableStateFlow<Set<Long>>(emptySet())
    private var recommendationsJob: Job? = null

    private val pageLoader = SearchPageLoader(
        getModelsUseCase = coreUseCases.getModels,
        multiSourceSearchUseCase = coreUseCases.multiSourceSearch,
        getViewedModelIdsUseCase = coreUseCases.getViewedModelIds,
        hiddenModelIds = _hiddenModelIds,
    )

    private val filterDelegate = SearchFilterDelegate(
        scope = viewModelScope,
        filterState = _filterState,
        hiddenModelIds = _hiddenModelIds,
        useCases = filterUseCases,
        updateFilter = { transform -> updateFilter(transform) },
        resetPaginationAndReload = ::refresh,
    )

    val searchHistory: StateFlow<List<String>> =
        historyUseCases.observeSearchHistory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val gridColumns: StateFlow<Int> =
        preferencesUseCases.observeGridColumns()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), 2)

    val ownedHashes: StateFlow<Set<String>> =
        favoritesUseCases.observeOwnedModelHashes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptySet())

    val favoriteIds: StateFlow<Set<Long>> =
        favoritesUseCases.observeFavorites()
            .map { favorites -> favorites.map { it.id }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptySet())

    val savedFilters: StateFlow<List<SavedSearchFilter>> =
        filterUseCases.observeSavedSearchFilters()
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
        observePreferences()
        filterDelegate.loadExcludedTags()
        loadDefaults(preferencesUseCases.observeDefaultSortOrder, preferencesUseCases.observeDefaultTimePeriod)
        refreshRecommendations()
    }

    // region Filter actions

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onSearch() {
        val query = _uiState.value.query
        if (query.isNotBlank()) {
            viewModelScope.launch { historyUseCases.addSearchHistory(query.trim()) }
        }
        updateFilter { it.copy(query = query) }
        refresh()
    }

    fun onHistoryItemClick(query: String) {
        _uiState.update { it.copy(query = query) }
        onSearch()
    }

    fun removeSearchHistoryItem(query: String) {
        viewModelScope.launch { historyUseCases.deleteSearchHistoryItem(query) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { historyUseCases.clearSearchHistory() }
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

    /**
     * Persists a new NSFW browsing level. The persisted preference is the single
     * source of truth: [observeNsfwFilter] picks up the change and triggers the
     * refresh and recommendations reload, so Settings and the filter sheet stay
     * in sync automatically.
     */
    fun onNsfwFilterLevelSelected(level: NsfwFilterLevel) {
        viewModelScope.launch { preferencesUseCases.setNsfwFilter(level) }
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
            suspendRunCatching { coreUseCases.trackRecommendationClick(modelId) }
        }
    }

    fun toggleFavorite(model: Model) {
        viewModelScope.launch {
            suspendRunCatching { favoritesUseCases.toggleFavorite(model) }
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
            // firstOrNull avoids NoSuchElementException if the preferences flow emits nothing;
            // fall back to the same defaults used by ModelSearchUiState/FilterState.
            val sort = observeSortUseCase().firstOrNull() ?: SortOrder.MostDownloaded
            val period = observePeriodUseCase().firstOrNull() ?: TimePeriod.AllTime
            _uiState.update { it.copy(selectedSort = sort, selectedPeriod = period) }
            val current = _filterState.value
            if (current.selectedSort != sort || current.selectedPeriod != period) {
                _filterState.update { it.copy(selectedSort = sort, selectedPeriod = period) }
            }
            // Initial load after defaults are set
            paginatedLoader.loadFirst()
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            var initialized = false
            preferencesUseCases.observeNsfwFilter().collect { level ->
                val prev = _uiState.value.nsfwFilterLevel
                _uiState.update { it.copy(nsfwFilterLevel = level) }
                _filterState.update { it.copy(nsfwFilterLevel = level) }
                if (initialized && prev != level) {
                    refreshRecommendations()
                    refresh()
                }
                initialized = true
            }
        }
        viewModelScope.launch {
            preferencesUseCases.observeQualityThreshold().collect { threshold ->
                _filterState.update { it.copy(qualityThreshold = threshold) }
            }
        }
    }

    /**
     * (Re)computes recommendations from the latest interaction signals. Called on init and
     * when returning from a detail view so a just-recorded click/view reshapes the feed.
     */
    fun refreshRecommendations() {
        recommendationsJob?.cancel()
        recommendationsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRecommendations = true) }
            try {
                val sections = coreUseCases.getRecommendations()
                _uiState.update {
                    it.copy(loadedRecommendations = sections, isLoadingRecommendations = false)
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
                hasActiveSearch = f.hasActiveSearch(),
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
