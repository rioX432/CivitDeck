package com.riox432.civitdeck.ui.analytics

import com.riox432.civitdeck.domain.model.CategoryStat
import com.riox432.civitdeck.domain.usecase.GetBrowsingStatsUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.util.suspendRunCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DesktopAnalyticsUiState(
    val isLoading: Boolean = true,
    val totalViews: Int = 0,
    val totalFavorites: Int = 0,
    val totalSearches: Int = 0,
    val averageViewDurationMs: Long? = null,
    val topModelTypes: List<CategoryStat> = emptyList(),
    val topCreators: List<CategoryStat> = emptyList(),
    val error: String? = null,
)

class DesktopAnalyticsViewModel(
    private val getBrowsingStatsUseCase: GetBrowsingStatsUseCase,
) : ViewModel() {

    private val scope = viewModelScope

    private val _uiState = MutableStateFlow(DesktopAnalyticsUiState())
    val uiState: StateFlow<DesktopAnalyticsUiState> = _uiState

    init {
        loadStats()
    }

    fun refresh() {
        loadStats()
    }

    private fun loadStats() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            suspendRunCatching { getBrowsingStatsUseCase() }
                .onSuccess { stats ->
                    _uiState.value = DesktopAnalyticsUiState(
                        isLoading = false,
                        totalViews = stats.totalViews,
                        totalFavorites = stats.totalFavorites,
                        totalSearches = stats.totalSearches,
                        averageViewDurationMs = stats.averageViewDurationMs,
                        topModelTypes = stats.topModelTypes,
                        topCreators = stats.topCreators,
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

    public override fun onCleared() {
        super.onCleared()
    }
}
