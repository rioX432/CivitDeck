package com.riox432.civitdeck.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.RecentlyViewedModel
import com.riox432.civitdeck.domain.usecase.ClearBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.DeleteBrowsingHistoryItemUseCase
import com.riox432.civitdeck.domain.usecase.ObserveRecentlyViewedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DateGroup(
    val label: String,
    val items: List<RecentlyViewedModel>,
)

data class BrowsingHistoryUiState(
    val groups: List<DateGroup> = emptyList(),
    val isEmpty: Boolean = true,
)

class BrowsingHistoryViewModel(
    private val observeRecentlyViewedUseCase: ObserveRecentlyViewedUseCase,
    private val deleteBrowsingHistoryItemUseCase: DeleteBrowsingHistoryItemUseCase,
    private val clearBrowsingHistoryUseCase: ClearBrowsingHistoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowsingHistoryUiState())
    val uiState: StateFlow<BrowsingHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeRecentlyViewedUseCase(limit = HISTORY_LIMIT).collect { items ->
                _uiState.update {
                    BrowsingHistoryUiState(
                        groups = groupByDate(items),
                        isEmpty = items.isEmpty(),
                    )
                }
            }
        }
    }

    fun deleteItem(historyId: Long) {
        viewModelScope.launch { deleteBrowsingHistoryItemUseCase(historyId) }
    }

    fun clearAll() {
        viewModelScope.launch { clearBrowsingHistoryUseCase() }
    }

    companion object {
        private const val HISTORY_LIMIT = 200
    }
}

private fun groupByDate(items: List<RecentlyViewedModel>): List<DateGroup> {
    val now = System.currentTimeMillis()
    val todayStart = now - (now % DAY_MILLIS)
    val yesterdayStart = todayStart - DAY_MILLIS
    val weekStart = todayStart - WEEK_MILLIS

    val today = mutableListOf<RecentlyViewedModel>()
    val yesterday = mutableListOf<RecentlyViewedModel>()
    val thisWeek = mutableListOf<RecentlyViewedModel>()
    val earlier = mutableListOf<RecentlyViewedModel>()

    items.forEach { item ->
        when {
            item.viewedAt >= todayStart -> today.add(item)
            item.viewedAt >= yesterdayStart -> yesterday.add(item)
            item.viewedAt >= weekStart -> thisWeek.add(item)
            else -> earlier.add(item)
        }
    }

    return buildList {
        if (today.isNotEmpty()) add(DateGroup("Today", today))
        if (yesterday.isNotEmpty()) add(DateGroup("Yesterday", yesterday))
        if (thisWeek.isNotEmpty()) add(DateGroup("This Week", thisWeek))
        if (earlier.isNotEmpty()) add(DateGroup("Earlier", earlier))
    }
}

private const val DAY_MILLIS = 86_400_000L
private const val WEEK_MILLIS = 7 * DAY_MILLIS
