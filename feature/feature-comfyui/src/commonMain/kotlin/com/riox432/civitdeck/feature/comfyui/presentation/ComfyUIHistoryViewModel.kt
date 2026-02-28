package com.riox432.civitdeck.feature.comfyui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.data.image.SaveGeneratedImageUseCase
import com.riox432.civitdeck.domain.model.ComfyUIGeneratedImage
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUIHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ComfyUIHistoryUiState(
    val images: List<ComfyUIGeneratedImage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedWorkflow: String? = null,
    val imageSaveSuccess: Boolean? = null,
)

class ComfyUIHistoryViewModel(
    private val fetchHistory: FetchComfyUIHistoryUseCase,
    private val saveImage: SaveGeneratedImageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComfyUIHistoryUiState())
    val uiState: StateFlow<ComfyUIHistoryUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            fetchHistory()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { images ->
                    _uiState.update {
                        it.copy(isLoading = false, images = images)
                    }
                }
        }
    }

    fun onSelectWorkflow(workflow: String?) {
        _uiState.update { it.copy(selectedWorkflow = workflow) }
    }

    fun onSaveImage(imageUrl: String, filename: String) {
        viewModelScope.launch {
            val success = saveImage(imageUrl, filename)
            _uiState.update { it.copy(imageSaveSuccess = success) }
        }
    }

    fun onDismissSaveResult() {
        _uiState.update { it.copy(imageSaveSuccess = null) }
    }

    fun filteredImages(): List<ComfyUIGeneratedImage> {
        val state = _uiState.value
        val workflow = state.selectedWorkflow ?: return state.images
        return state.images.filter { it.meta.samplerName == workflow }
    }
}
