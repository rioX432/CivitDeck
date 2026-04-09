package com.riox432.civitdeck.presentation.comfyhub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.ComfyHubCategory
import com.riox432.civitdeck.domain.model.ComfyHubSortOrder
import com.riox432.civitdeck.domain.model.ComfyHubWorkflow
import com.riox432.civitdeck.domain.util.UiLoadingState
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SearchComfyHubWorkflowsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ComfyHubBrowserUiState(
    val workflows: List<ComfyHubWorkflow> = emptyList(),
    override val isLoading: Boolean = true,
    override val error: String? = null,
    val query: String = "",
    val selectedCategory: ComfyHubCategory = ComfyHubCategory.ALL,
    val sortOrder: ComfyHubSortOrder = ComfyHubSortOrder.MOST_DOWNLOADED,
) : UiLoadingState

class ComfyHubBrowserViewModel(
    private val searchWorkflows: SearchComfyHubWorkflowsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComfyHubBrowserUiState())
    val uiState: StateFlow<ComfyHubBrowserUiState> = _uiState.asStateFlow()

    init {
        search()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        search()
    }

    fun onCategorySelected(category: ComfyHubCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
        search()
    }

    fun onSortOrderChanged(sort: ComfyHubSortOrder) {
        _uiState.update { it.copy(sortOrder = sort) }
        search()
    }

    fun retry() {
        search()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun search() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val state = _uiState.value
                val results = searchWorkflows(
                    query = state.query,
                    category = state.selectedCategory,
                    sort = state.sortOrder,
                )
                _uiState.update { it.copy(workflows = results, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Search failed", isLoading = false)
                }
            }
        }
    }
}
