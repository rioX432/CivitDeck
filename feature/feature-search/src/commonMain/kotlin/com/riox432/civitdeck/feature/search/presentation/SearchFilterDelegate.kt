package com.riox432.civitdeck.feature.search.presentation

import com.riox432.civitdeck.domain.model.SavedSearchFilter
import com.riox432.civitdeck.domain.usecase.AddExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.GetExcludedTagsUseCase
import com.riox432.civitdeck.domain.usecase.RemoveExcludedTagUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.DeleteSavedSearchFilterUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetHiddenModelIdsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.HideModelUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.SaveSearchFilterUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Delegate handling excluded tags, hidden models, and saved filter operations
 * extracted from [ModelSearchViewModel] to stay under detekt's function-count threshold.
 */
@Suppress("LongParameterList")
internal class SearchFilterDelegate(
    private val scope: CoroutineScope,
    private val filterState: MutableStateFlow<FilterState>,
    private val hiddenModelIds: MutableStateFlow<Set<Long>>,
    private val getExcludedTagsUseCase: GetExcludedTagsUseCase,
    private val addExcludedTagUseCase: AddExcludedTagUseCase,
    private val removeExcludedTagUseCase: RemoveExcludedTagUseCase,
    private val getHiddenModelIdsUseCase: GetHiddenModelIdsUseCase,
    private val hideModelUseCase: HideModelUseCase,
    private val saveSearchFilterUseCase: SaveSearchFilterUseCase,
    private val deleteSavedSearchFilterUseCase: DeleteSavedSearchFilterUseCase,
    private val updateFilter: (transform: (FilterState) -> FilterState) -> Unit,
    private val resetPaginationAndReload: () -> Unit,
) {

    fun onAddExcludedTag(tag: String) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isBlank()) return
        scope.launch {
            addExcludedTagUseCase(trimmed)
            loadExcludedTags()
            resetPaginationAndReload()
        }
    }

    fun onRemoveExcludedTag(tag: String) {
        scope.launch {
            removeExcludedTagUseCase(tag)
            loadExcludedTags()
            resetPaginationAndReload()
        }
    }

    fun onHideModel(modelId: Long, modelName: String) {
        scope.launch {
            hideModelUseCase(modelId, modelName)
            val ids = getHiddenModelIdsUseCase()
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
        scope.launch { saveSearchFilterUseCase(name, toSave) }
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
        scope.launch { deleteSavedSearchFilterUseCase(id) }
    }

    fun loadExcludedTags() {
        scope.launch {
            val tags = getExcludedTagsUseCase()
            val hiddenIds = getHiddenModelIdsUseCase()
            hiddenModelIds.value = hiddenIds
            updateFilter { it.copy(excludedTags = tags) }
        }
    }
}
