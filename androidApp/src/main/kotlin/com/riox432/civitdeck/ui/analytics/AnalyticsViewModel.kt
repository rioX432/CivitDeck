package com.riox432.civitdeck.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.CategoryStat
import com.riox432.civitdeck.domain.model.DailyViewCount
import com.riox432.civitdeck.domain.usecase.GetBrowsingStatsUseCase
import com.riox432.civitdeck.domain.util.suspendRunCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val totalViews: Int = 0,
    val totalFavorites: Int = 0,
    val totalSearches: Int = 0,
    val averageViewDurationMs: Long? = null,
    val dailyViewCounts: List<DailyViewCount> = emptyList(),
    val topModelTypes: List<CategoryStat> = emptyList(),
    val topCreators: List<CategoryStat> = emptyList(),
    val topSearchQueries: List<CategoryStat> = emptyList(),
    val error: String? = null,
)

class AnalyticsViewModel(
    private val getBrowsingStatsUseCase: GetBrowsingStatsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState

    init {
        loadStats()
    }

    fun refresh() {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            suspendRunCatching { getBrowsingStatsUseCase() }
                .onSuccess { stats ->
                    _uiState.value = AnalyticsUiState(
                        isLoading = false,
                        totalViews = stats.totalViews,
                        totalFavorites = stats.totalFavorites,
                        totalSearches = stats.totalSearches,
                        averageViewDurationMs = stats.averageViewDurationMs,
                        dailyViewCounts = stats.dailyViewCounts,
                        topModelTypes = stats.topModelTypes,
                        topCreators = stats.topCreators,
                        topSearchQueries = stats.topSearchQueries,
                    )
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load stats",
                        )
                    }
                }
        }
    }
}
