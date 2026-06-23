package com.riox432.civitdeck.feature.search.presentation

import com.riox432.civitdeck.domain.model.SavedSearchFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Delegate handling excluded tags, hidden models, and saved filter operations
 * extracted from [ModelSearchViewModel] to stay under detekt's function-count threshold.
 */
internal class SearchFilterDelegate(
    private val scope: CoroutineScope,
    private val filterState: MutableStateFlow<FilterState>,
    private val hiddenModelIds: MutableStateFlow<Set<Long>>,
    private val useCases: SearchFilterUseCases,
    private val updateFilter: (transform: (FilterState) -> FilterState) -> Unit,
    private val resetPaginationAndReload: () -> Unit,
) {

    fun onAddExcludedTag(tag: String) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isBlank()) return
        scope.launch {
            useCases.addExcludedTag(trimmed)
            loadExcludedTags()
            resetPaginationAndReload()
        }
    }

    fun onRemoveExcludedTag(tag: String) {
        scope.launch {
            useCases.removeExcludedTag(tag)
            loadExcludedTags()
            resetPaginationAndReload()
        }
    }

    fun onHideModel(modelId: Long, modelName: String) {
        scope.launch {
            useCases.hideModel(modelId, modelName)
            val ids = useCases.getHiddenModelIds()
            hiddenModelIds.value = ids
        }
    }

    fun saveCurrentFilter(name: String) {
        val filter = filterState.value
        val toSave = SavedSearchFilter(
            id = 0,
            name = name,
            query = filter.query,
            selectedType = filter.selectedType,
            selectedSort = filter.selectedSort,
            selectedPeriod = filter.selectedPeriod,
            selectedBaseModels = filter.selectedBaseModels,
            nsfwFilterLevel = filter.nsfwFilterLevel,
            isFreshFindEnabled = filter.isFreshFindEnabled,
            excludedTags = filter.excludedTags,
            includedTags = filter.includedTags,
            selectedSources = filter.selectedSources,
            savedAt = 0,
        )
        scope.launch { useCases.saveSearchFilter(name, toSave) }
    }

    fun applyFilter(filter: SavedSearchFilter) {
        updateFilter {
            it.copy(
                query = filter.query,
                selectedType = filter.selectedType,
                selectedSort = filter.selectedSort,
                selectedPeriod = filter.selectedPeriod,
                selectedBaseModels = filter.selectedBaseModels,
                nsfwFilterLevel = filter.nsfwFilterLevel,
                isFreshFindEnabled = filter.isFreshFindEnabled,
                includedTags = filter.includedTags,
                excludedTags = filter.excludedTags,
                selectedSources = filter.selectedSources,
            )
        }
        resetPaginationAndReload()
    }

    fun deleteSavedFilter(id: Long) {
        scope.launch { useCases.deleteSavedSearchFilter(id) }
    }

    fun loadExcludedTags() {
        scope.launch {
            val tags = useCases.getExcludedTags()
            val hiddenIds = useCases.getHiddenModelIds()
            hiddenModelIds.value = hiddenIds
            updateFilter { it.copy(excludedTags = tags) }
        }
    }
}
