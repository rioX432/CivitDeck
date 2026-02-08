package com.riox432.civitdeck.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ModelDetailUiState(
    val model: Model? = null,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedVersionIndex: Int = 0,
)

class ModelDetailViewModel(
    private val modelId: Long,
    private val getModelDetailUseCase: GetModelDetailUseCase,
    private val observeIsFavoriteUseCase: ObserveIsFavoriteUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val trackModelViewUseCase: TrackModelViewUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelDetailUiState())
    val uiState: StateFlow<ModelDetailUiState> = _uiState.asStateFlow()

    init {
        loadModel()
        observeFavorite()
    }

    fun onVersionSelected(index: Int) {
        _uiState.update { it.copy(selectedVersionIndex = index) }
    }

    fun onFavoriteToggle() {
        val model = _uiState.value.model ?: return
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase(model)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Favorite toggle failure is non-critical
            }
        }
    }

    fun retry() {
        loadModel()
    }

    private fun loadModel() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val model = getModelDetailUseCase(modelId)
                _uiState.update {
                    it.copy(model = model, isLoading = false)
                }
                trackModelViewUseCase(
                    modelId = model.id,
                    modelType = model.type.name,
                    creatorName = model.creator?.username,
                    tags = model.tags,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Unknown error")
                }
            }
        }
    }

    private fun observeFavorite() {
        viewModelScope.launch {
            observeIsFavoriteUseCase(modelId).collect { isFavorite ->
                _uiState.update { it.copy(isFavorite = isFavorite) }
            }
        }
    }
}
