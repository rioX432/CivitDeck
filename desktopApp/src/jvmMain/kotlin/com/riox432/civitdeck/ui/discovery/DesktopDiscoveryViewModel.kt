package com.riox432.civitdeck.ui.discovery

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.RecommendationSection
import com.riox432.civitdeck.feature.search.domain.usecase.GetRecommendationsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DesktopDiscoveryUiState(
    val sections: List<RecommendationSection> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class DesktopDiscoveryViewModel(
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _uiState = MutableStateFlow(DesktopDiscoveryUiState())
    val uiState: StateFlow<DesktopDiscoveryUiState> = _uiState

    init {
        loadRecommendations()
    }

    fun refresh() {
        loadRecommendations()
    }

    private fun loadRecommendations() {
        scope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
            )
            try {
                val sections = getRecommendationsUseCase()
                _uiState.value = _uiState.value.copy(
                    sections = sections,
                    isLoading = false,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load recommendations",
                )
            }
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
