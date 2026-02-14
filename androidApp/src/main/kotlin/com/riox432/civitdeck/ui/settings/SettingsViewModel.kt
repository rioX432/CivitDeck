package com.riox432.civitdeck.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.data.local.entity.HiddenModelEntity
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.usecase.AddExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.ClearBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ClearCacheUseCase
import com.riox432.civitdeck.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.GetExcludedTagsUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.RemoveExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.SetGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.SetNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.UnhideModelUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val nsfwFilterLevel: NsfwFilterLevel = NsfwFilterLevel.Off,
    val defaultSortOrder: SortOrder = SortOrder.MostDownloaded,
    val defaultTimePeriod: TimePeriod = TimePeriod.AllTime,
    val gridColumns: Int = 2,
    val hiddenModels: List<HiddenModelEntity> = emptyList(),
    val excludedTags: List<String> = emptyList(),
)

@Suppress("LongParameterList")
class SettingsViewModel(
    observeNsfwFilterUseCase: ObserveNsfwFilterUseCase,
    private val setNsfwFilterUseCase: SetNsfwFilterUseCase,
    observeDefaultSortOrderUseCase: ObserveDefaultSortOrderUseCase,
    private val setDefaultSortOrderUseCase: SetDefaultSortOrderUseCase,
    observeDefaultTimePeriodUseCase: ObserveDefaultTimePeriodUseCase,
    private val setDefaultTimePeriodUseCase: SetDefaultTimePeriodUseCase,
    observeGridColumnsUseCase: ObserveGridColumnsUseCase,
    private val setGridColumnsUseCase: SetGridColumnsUseCase,
    private val getHiddenModelsUseCase: GetHiddenModelsUseCase,
    private val unhideModelUseCase: UnhideModelUseCase,
    private val getExcludedTagsUseCase: GetExcludedTagsUseCase,
    private val addExcludedTagUseCase: AddExcludedTagUseCase,
    private val removeExcludedTagUseCase: RemoveExcludedTagUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase,
    private val clearBrowsingHistoryUseCase: ClearBrowsingHistoryUseCase,
    private val clearCacheUseCase: ClearCacheUseCase,
) : ViewModel() {

    private val _mutableState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = combine(
        observeNsfwFilterUseCase(),
        observeDefaultSortOrderUseCase(),
        observeDefaultTimePeriodUseCase(),
        observeGridColumnsUseCase(),
        _mutableState,
    ) { nsfw, sort, period, columns, mutable ->
        mutable.copy(
            nsfwFilterLevel = nsfw,
            defaultSortOrder = sort,
            defaultTimePeriod = period,
            gridColumns = columns,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        loadMutableData()
    }

    private fun loadMutableData() {
        viewModelScope.launch {
            val hidden = getHiddenModelsUseCase()
            val tags = getExcludedTagsUseCase()
            _mutableState.update { it.copy(hiddenModels = hidden, excludedTags = tags) }
        }
    }

    fun onNsfwFilterChanged(level: NsfwFilterLevel) {
        viewModelScope.launch { setNsfwFilterUseCase(level) }
    }

    fun onSortOrderChanged(sort: SortOrder) {
        viewModelScope.launch { setDefaultSortOrderUseCase(sort) }
    }

    fun onTimePeriodChanged(period: TimePeriod) {
        viewModelScope.launch { setDefaultTimePeriodUseCase(period) }
    }

    fun onGridColumnsChanged(columns: Int) {
        viewModelScope.launch { setGridColumnsUseCase(columns) }
    }

    fun onUnhideModel(modelId: Long) {
        viewModelScope.launch {
            unhideModelUseCase(modelId)
            val hidden = getHiddenModelsUseCase()
            _mutableState.update { it.copy(hiddenModels = hidden) }
        }
    }

    fun onAddExcludedTag(tag: String) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            addExcludedTagUseCase(trimmed)
            val tags = getExcludedTagsUseCase()
            _mutableState.update { it.copy(excludedTags = tags) }
        }
    }

    fun onRemoveExcludedTag(tag: String) {
        viewModelScope.launch {
            removeExcludedTagUseCase(tag)
            val tags = getExcludedTagsUseCase()
            _mutableState.update { it.copy(excludedTags = tags) }
        }
    }

    fun onClearSearchHistory() {
        viewModelScope.launch { clearSearchHistoryUseCase() }
    }

    fun onClearBrowsingHistory() {
        viewModelScope.launch { clearBrowsingHistoryUseCase() }
    }

    fun onClearCache() {
        viewModelScope.launch { clearCacheUseCase() }
    }
}
