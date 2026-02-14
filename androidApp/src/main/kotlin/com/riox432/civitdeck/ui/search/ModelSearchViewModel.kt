package com.riox432.civitdeck.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.RecommendationSection
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.usecase.AddExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.AddSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.GetExcludedTagsUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetRecommendationsUseCase
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.HideModelUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObserveSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.RemoveExcludedTagUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
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
    val recommendations: List<RecommendationSection> = emptyList(),
    val isLoadingRecommendations: Boolean = false,
    val excludedTags: List<String> = emptyList(),
    val includedTags: List<String> = emptyList(),
)

internal data class FilterState(
    val query: String = "",
    val selectedType: ModelType? = null,
    val selectedSort: SortOrder = SortOrder.MostDownloaded,
    val selectedPeriod: TimePeriod = TimePeriod.AllTime,
    val selectedBaseModels: Set<BaseModel> = emptySet(),
    val nsfwFilterLevel: NsfwFilterLevel = NsfwFilterLevel.Off,
    val isFreshFindEnabled: Boolean = false,
    val excludedTags: List<String> = emptyList(),
    val includedTags: List<String> = emptyList(),
)

@Suppress("LongParameterList")
class ModelSearchViewModel(
    private val getModelsUseCase: GetModelsUseCase,
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val observeNsfwFilterUseCase: ObserveNsfwFilterUseCase,
    observeSearchHistoryUseCase: ObserveSearchHistoryUseCase,
    private val addSearchHistoryUseCase: AddSearchHistoryUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase,
    private val getViewedModelIdsUseCase: GetViewedModelIdsUseCase,
    private val getExcludedTagsUseCase: GetExcludedTagsUseCase,
    private val addExcludedTagUseCase: AddExcludedTagUseCase,
    private val removeExcludedTagUseCase: RemoveExcludedTagUseCase,
    private val getHiddenModelIdsUseCase: GetHiddenModelIdsUseCase,
    private val hideModelUseCase: HideModelUseCase,
    observeGridColumnsUseCase: ObserveGridColumnsUseCase,
    observeDefaultSortOrderUseCase: ObserveDefaultSortOrderUseCase,
    observeDefaultTimePeriodUseCase: ObserveDefaultTimePeriodUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelSearchUiState())
    val uiState: StateFlow<ModelSearchUiState> = _uiState.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    private val _hiddenModelIds = MutableStateFlow<Set<Long>>(emptySet())

    val searchHistory: StateFlow<List<String>> =
        observeSearchHistoryUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val gridColumns: StateFlow<Int> =
        observeGridColumnsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 2)

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingData: Flow<PagingData<Model>> = combine(
        _filterState,
        _hiddenModelIds,
    ) { filters, hiddenIds ->
        filters to hiddenIds
    }
        .flatMapLatest { (filters, hiddenIds) ->
            Pager(
                config = PagingConfig(
                    pageSize = PAGE_SIZE,
                    prefetchDistance = PAGE_SIZE * 2,
                    initialLoadSize = PAGE_SIZE * 3,
                ),
                pagingSourceFactory = {
                    ModelPagingSource(
                        getModelsUseCase = getModelsUseCase,
                        getViewedModelIdsUseCase = getViewedModelIdsUseCase,
                        filterState = filters,
                        hiddenModelIds = hiddenIds,
                    )
                },
            ).flow
        }
        .cachedIn(viewModelScope)

    init {
        observeNsfwFilter()
        loadExcludedTags()
        loadDefaults(observeDefaultSortOrderUseCase, observeDefaultTimePeriodUseCase)
        loadRecommendations()
    }

    private fun loadDefaults(
        observeSortUseCase: ObserveDefaultSortOrderUseCase,
        observePeriodUseCase: ObserveDefaultTimePeriodUseCase,
    ) {
        viewModelScope.launch {
            val sort = observeSortUseCase().first()
            val period = observePeriodUseCase().first()
            _uiState.update { it.copy(selectedSort = sort, selectedPeriod = period) }
            _filterState.update { it.copy(selectedSort = sort, selectedPeriod = period) }
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
                }
            }
        }
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRecommendations = true) }
            try {
                val sections = getRecommendationsUseCase()
                _uiState.update {
                    it.copy(recommendations = sections, isLoadingRecommendations = false)
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingRecommendations = false) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onSearch() {
        val query = _uiState.value.query
        if (query.isNotBlank()) {
            viewModelScope.launch { addSearchHistoryUseCase(query.trim()) }
        }
        _filterState.update { it.copy(query = query) }
    }

    fun onHistoryItemClick(query: String) {
        _uiState.update { it.copy(query = query) }
        onSearch()
    }

    fun clearSearchHistory() {
        viewModelScope.launch { clearSearchHistoryUseCase() }
    }

    fun resetFilters() {
        _uiState.update {
            it.copy(
                selectedType = null,
                selectedSort = SortOrder.MostDownloaded,
                selectedPeriod = TimePeriod.AllTime,
                selectedBaseModels = emptySet(),
                isFreshFindEnabled = false,
                includedTags = emptyList(),
            )
        }
        _filterState.update {
            it.copy(
                selectedType = null,
                selectedSort = SortOrder.MostDownloaded,
                selectedPeriod = TimePeriod.AllTime,
                selectedBaseModels = emptySet(),
                isFreshFindEnabled = false,
                includedTags = emptyList(),
            )
        }
    }

    fun onTypeSelected(type: ModelType?) {
        _uiState.update { it.copy(selectedType = type) }
        _filterState.update { it.copy(selectedType = type) }
    }

    fun onSortSelected(sort: SortOrder) {
        _uiState.update { it.copy(selectedSort = sort) }
        _filterState.update { it.copy(selectedSort = sort) }
    }

    fun onPeriodSelected(period: TimePeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        _filterState.update { it.copy(selectedPeriod = period) }
    }

    fun onBaseModelToggled(baseModel: BaseModel) {
        val updater = { current: Set<BaseModel> ->
            current.toMutableSet().apply {
                if (baseModel in this) remove(baseModel) else add(baseModel)
            }.toSet()
        }
        _uiState.update { it.copy(selectedBaseModels = updater(it.selectedBaseModels)) }
        _filterState.update { it.copy(selectedBaseModels = updater(it.selectedBaseModels)) }
    }

    fun onFreshFindToggled() {
        _uiState.update { it.copy(isFreshFindEnabled = !it.isFreshFindEnabled) }
        _filterState.update { it.copy(isFreshFindEnabled = !it.isFreshFindEnabled) }
    }

    fun onAddIncludedTag(tag: String) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isBlank()) return
        val current = _uiState.value.includedTags
        if (trimmed in current) return
        _uiState.update { it.copy(includedTags = it.includedTags + trimmed) }
        _filterState.update { it.copy(includedTags = it.includedTags + trimmed) }
    }

    fun onRemoveIncludedTag(tag: String) {
        _uiState.update { it.copy(includedTags = it.includedTags - tag) }
        _filterState.update { it.copy(includedTags = it.includedTags - tag) }
    }

    fun onAddExcludedTag(tag: String) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            addExcludedTagUseCase(trimmed)
            loadExcludedTags()
        }
    }

    fun onRemoveExcludedTag(tag: String) {
        viewModelScope.launch {
            removeExcludedTagUseCase(tag)
            loadExcludedTags()
        }
    }

    fun onHideModel(modelId: Long, modelName: String) {
        viewModelScope.launch {
            hideModelUseCase(modelId, modelName)
            val ids = getHiddenModelIdsUseCase()
            _hiddenModelIds.value = ids
        }
    }

    private fun loadExcludedTags() {
        viewModelScope.launch {
            val tags = getExcludedTagsUseCase()
            val hiddenIds = getHiddenModelIdsUseCase()
            _hiddenModelIds.value = hiddenIds
            _uiState.update { it.copy(excludedTags = tags) }
            _filterState.update { it.copy(excludedTags = tags) }
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
