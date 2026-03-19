package com.riox432.civitdeck.ui.similar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.GetSimilarModelsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SimilarModelsUiState(
    val sourceModel: Model? = null,
    val similarModels: List<Model> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class SimilarModelsViewModel(
    private val modelId: Long,
    private val getModelDetail: GetModelDetailUseCase,
    private val getSimilarModels: GetSimilarModelsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SimilarModelsUiState())
    val uiState: StateFlow<SimilarModelsUiState> = _uiState.asStateFlow()

    init {
        loadSimilarModels()
    }

    fun retry() {
        loadSimilarModels()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun loadSimilarModels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val source = getModelDetail(modelId)
                _uiState.update { it.copy(sourceModel = source) }
                val similar = getSimilarModels(source)
                _uiState.update { it.copy(similarModels = similar, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load similar models", isLoading = false) }
            }
        }
    }
}
