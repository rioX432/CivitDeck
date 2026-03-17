package com.riox432.civitdeck.ui.analytics

import com.riox432.civitdeck.domain.model.CategoryStat
import com.riox432.civitdeck.domain.usecase.GetBrowsingStatsUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DesktopAnalyticsUiState(
    val isLoading: Boolean = true,
    val totalViews: Int = 0,
    val totalFavorites: Int = 0,
    val totalSearches: Int = 0,
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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val stats = getBrowsingStatsUseCase()
                _uiState.value = DesktopAnalyticsUiState(
                    isLoading = false,
                    totalViews = stats.totalViews,
                    totalFavorites = stats.totalFavorites,
                    totalSearches = stats.totalSearches,
                    topModelTypes = stats.topModelTypes,
                    topCreators = stats.topCreators,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load stats",
                )
            }
        }
    }

    public override fun onCleared() {
        super.onCleared()
    }
}
