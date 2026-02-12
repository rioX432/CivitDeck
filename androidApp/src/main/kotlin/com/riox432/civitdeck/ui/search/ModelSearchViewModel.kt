package com.riox432.civitdeck.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObserveSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.RemoveExcludedTagUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
    val isFreshFindEnabled: Boolean = false,
    val recommendations: List<RecommendationSection> = emptyList(),
    val isLoadingRecommendations: Boolean = false,
    val excludedTags: List<String> = emptyList(),
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelSearchUiState())
    val uiState: StateFlow<ModelSearchUiState> = _uiState.asStateFlow()

    val searchHistory: StateFlow<List<String>> =
        observeSearchHistoryUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var loadJob: Job? = null
    private var hiddenModelIds: Set<Long> = emptySet()

    init {
        observeNsfwFilter()
        loadExcludedTags()
        loadModels()
        loadRecommendations()
    }

    private fun observeNsfwFilter() {
        viewModelScope.launch {
            observeNsfwFilterUseCase().collect { level ->
                val prev = _uiState.value.nsfwFilterLevel
                _uiState.update { it.copy(nsfwFilterLevel = level) }
                if (prev != level) {
                    loadJob?.cancel()
                    _uiState.update {
                        it.copy(nextCursor = null, models = emptyList(), hasMore = true)
                    }
                    loadModels()
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
        loadJob?.cancel()
        _uiState.update {
            it.copy(nextCursor = null, models = emptyList(), hasMore = true)
        }
        loadModels()
    }

    fun onHistoryItemClick(query: String) {
        _uiState.update { it.copy(query = query) }
        onSearch()
    }

    fun clearSearchHistory() {
        viewModelScope.launch { clearSearchHistoryUseCase() }
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

    fun onFreshFindToggled() {
        loadJob?.cancel()
        _uiState.update {
            it.copy(
                isFreshFindEnabled = !it.isFreshFindEnabled,
                nextCursor = null,
                models = emptyList(),
                hasMore = true,
            )
        }
        loadModels()
    }

    fun onAddExcludedTag(tag: String) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            addExcludedTagUseCase(trimmed)
            loadExcludedTags()
            reloadModels()
        }
    }

    fun onRemoveExcludedTag(tag: String) {
        viewModelScope.launch {
            removeExcludedTagUseCase(tag)
            loadExcludedTags()
            reloadModels()
        }
    }

    fun onHideModel(modelId: Long, modelName: String) {
        viewModelScope.launch {
            hideModelUseCase(modelId, modelName)
            hiddenModelIds = getHiddenModelIdsUseCase()
            _uiState.update { it.copy(models = it.models.filter { m -> m.id != modelId }) }
        }
    }

    private fun loadExcludedTags() {
        viewModelScope.launch {
            val tags = getExcludedTagsUseCase()
            hiddenModelIds = getHiddenModelIdsUseCase()
            _uiState.update { it.copy(excludedTags = tags) }
        }
    }

    private fun reloadModels() {
        loadJob?.cancel()
        _uiState.update { it.copy(nextCursor = null, models = emptyList(), hasMore = true) }
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
                var filteredItems = filterNsfw(result.items, state.nsfwFilterLevel)
                if (state.isFreshFindEnabled) {
                    val viewedIds = getViewedModelIdsUseCase()
                    filteredItems = filteredItems.filter { it.id !in viewedIds }
                }
                if (state.excludedTags.isNotEmpty()) {
                    val excluded = state.excludedTags.toSet()
                    filteredItems = filteredItems.filter { model ->
                        model.tags.none { it.lowercase() in excluded }
                    }
                }
                if (hiddenModelIds.isNotEmpty()) {
                    filteredItems = filteredItems.filter { it.id !in hiddenModelIds }
                }
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
