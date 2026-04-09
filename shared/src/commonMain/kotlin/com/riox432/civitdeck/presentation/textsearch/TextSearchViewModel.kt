package com.riox432.civitdeck.presentation.textsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.TextSearchUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TextSearchUiState(
    val query: String = "",
    val results: List<Model> = emptyList(),
    val isLoading: Boolean = false,
    val isModelAvailable: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false,
)

/**
 * Drives the "AI Search" screen — natural language text-to-image search using
 * SigLIP-2's shared text/image embedding space.
 *
 * Flow:
 *  1. User types a description (e.g. "anime girl with blue hair").
 *  2. [TextSearchUseCase] produces a text embedding and runs cosine similarity
 *     against cached image embeddings.
 *  3. Hits are resolved to full [Model] objects via parallel detail lookups.
 *
 * When the text encoder is not yet available ([TextSearchUseCase.isAvailable] = false),
 * the UI shows a "coming soon" empty state instead of a search bar.
 */
class TextSearchViewModel(
    private val textSearchUseCase: TextSearchUseCase,
    private val getModelDetail: GetModelDetailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TextSearchUiState(isModelAvailable = textSearchUseCase.isAvailable),
    )
    val uiState: StateFlow<TextSearchUiState> = _uiState.asStateFlow()

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        viewModelScope.launch { executeSearch(query) }
    }

    fun retry() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        viewModelScope.launch { executeSearch(query) }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun executeSearch(query: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val hits = textSearchUseCase(query)
            val resolved: List<Model> = coroutineScope {
                hits
                    .map { hit -> async { runCatching { getModelDetail(hit.modelId) }.getOrNull() } }
                    .awaitAll()
                    .filterNotNull()
            }
            _uiState.update { it.copy(results = resolved, isLoading = false, hasSearched = true) }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    error = e.message ?: "Search failed",
                    isLoading = false,
                    hasSearched = true,
                )
            }
        }
    }
}
