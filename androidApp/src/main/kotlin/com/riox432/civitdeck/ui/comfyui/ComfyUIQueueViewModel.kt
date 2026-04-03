package com.riox432.civitdeck.ui.comfyui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.QueueJob
import com.riox432.civitdeck.feature.comfyui.domain.usecase.CancelComfyUIJobUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveComfyUIQueueUseCase
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QueueUiState(
    val jobs: List<QueueJob> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val cancellingIds: Set<String> = emptySet(),
)

class ComfyUIQueueViewModel(
    private val observeQueue: ObserveComfyUIQueueUseCase,
    private val cancelJob: CancelComfyUIJobUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QueueUiState())
    val uiState: StateFlow<QueueUiState> = _uiState

    init {
        startObservingQueue()
    }

    private fun startObservingQueue() {
        viewModelScope.launch {
            observeQueue()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { jobs ->
                    _uiState.update { it.copy(jobs = jobs, isLoading = false, error = null) }
                }
        }
    }

    fun onCancelJob(promptId: String) {
        if (promptId in _uiState.value.cancellingIds) return
        _uiState.update { it.copy(cancellingIds = it.cancellingIds + promptId) }
        viewModelScope.launch {
            try {
                cancelJob(promptId)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.e(TAG, "Failed to cancel job $promptId: ${e.message}")
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(cancellingIds = it.cancellingIds - promptId) }
            }
        }
    }

    companion object {
        private const val TAG = "ComfyUIQueueVM"
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
