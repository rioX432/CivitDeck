package com.riox432.civitdeck.feature.comfyui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.GenerationResult
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.repository.ComfyUIRepository
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUICheckpointsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveGenerationProgressUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.PollComfyUIResultUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SubmitComfyUIGenerationUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GenerationUiState(
    val checkpoints: List<String> = emptyList(),
    val selectedCheckpoint: String = "",
    val prompt: String = "",
    val negativePrompt: String = "",
    val steps: Int = 20,
    val cfgScale: Double = 7.0,
    val width: Int = 512,
    val height: Int = 512,
    val seed: Long = -1,
    val samplerName: String = "euler",
    val scheduler: String = "normal",
    val isLoadingCheckpoints: Boolean = false,
    val generationStatus: GenerationStatus = GenerationStatus.Idle,
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val result: GenerationResult? = null,
    val error: String? = null,
) {
    /** Progress fraction in [0f, 1f]. Returns 0 when totalSteps is unknown. */
    val progressFraction: Float
        get() = if (totalSteps > 0) currentStep.toFloat() / totalSteps.toFloat() else 0f
}

class ComfyUIGenerationViewModel(
    private val fetchCheckpoints: FetchComfyUICheckpointsUseCase,
    private val submitGeneration: SubmitComfyUIGenerationUseCase,
    private val pollResult: PollComfyUIResultUseCase,
    private val observeProgress: ObserveGenerationProgressUseCase,
    private val repository: ComfyUIRepository,
) : ViewModel() {

    private var progressJob: Job? = null

    private val _uiState = MutableStateFlow(GenerationUiState())
    val uiState: StateFlow<GenerationUiState> = _uiState

    init {
        loadCheckpoints()
    }

    private fun loadCheckpoints() {
        _uiState.update { it.copy(isLoadingCheckpoints = true) }
        viewModelScope.launch {
            try {
                val list = fetchCheckpoints()
                _uiState.update {
                    it.copy(
                        checkpoints = list,
                        selectedCheckpoint = list.firstOrNull() ?: "",
                        isLoadingCheckpoints = false,
                    )
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                _uiState.update {
                    it.copy(isLoadingCheckpoints = false, error = e.message)
                }
            }
        }
    }

    fun onCheckpointSelected(checkpoint: String) {
        _uiState.update { it.copy(selectedCheckpoint = checkpoint) }
    }

    fun onPromptChanged(prompt: String) {
        _uiState.update { it.copy(prompt = prompt) }
    }

    fun onNegativePromptChanged(prompt: String) {
        _uiState.update { it.copy(negativePrompt = prompt) }
    }

    fun onStepsChanged(steps: Int) {
        _uiState.update { it.copy(steps = steps) }
    }

    fun onCfgScaleChanged(cfg: Double) {
        _uiState.update { it.copy(cfgScale = cfg) }
    }

    fun onWidthChanged(width: Int) {
        _uiState.update { it.copy(width = width) }
    }

    fun onHeightChanged(height: Int) {
        _uiState.update { it.copy(height = height) }
    }

    fun onSeedChanged(seed: Long) {
        _uiState.update { it.copy(seed = seed) }
    }

    fun onGenerate() {
        val state = _uiState.value
        if (state.selectedCheckpoint.isBlank() || state.prompt.isBlank()) return
        progressJob?.cancel()
        _uiState.update {
            it.copy(
                generationStatus = GenerationStatus.Submitting,
                error = null,
                result = null,
                currentStep = 0,
                totalSteps = 0,
            )
        }
        viewModelScope.launch {
            try {
                val promptId = submitGeneration(buildParams(state))
                _uiState.update { it.copy(generationStatus = GenerationStatus.Running) }
                val connection = repository.getActiveConnection()
                if (connection != null) {
                    startWebSocketProgress(promptId, connection)
                } else {
                    pollForResult(promptId)
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                _uiState.update {
                    it.copy(generationStatus = GenerationStatus.Error, error = e.message)
                }
            }
        }
    }

    private fun buildParams(state: GenerationUiState) = ComfyUIGenerationParams(
        checkpoint = state.selectedCheckpoint,
        prompt = state.prompt,
        negativePrompt = state.negativePrompt,
        steps = state.steps,
        cfgScale = state.cfgScale,
        seed = state.seed,
        width = state.width,
        height = state.height,
        samplerName = state.samplerName,
        scheduler = state.scheduler,
    )

    private fun startWebSocketProgress(promptId: String, connection: ComfyUIConnection) {
        progressJob = viewModelScope.launch {
            observeProgress(promptId, connection.hostname, connection.port)
                .catch { pollForResult(promptId) }
                .collect { progress ->
                    _uiState.update {
                        it.copy(currentStep = progress.currentStep, totalSteps = progress.totalSteps)
                    }
                }
            // WebSocket flow completed: fetch final result
            fetchFinalResult(promptId)
        }
    }

    private suspend fun fetchFinalResult(promptId: String) {
        try {
            val result = pollResult(promptId)
            _uiState.update {
                if (result.status == GenerationStatus.Completed) {
                    it.copy(generationStatus = GenerationStatus.Completed, result = result)
                } else {
                    it.copy(generationStatus = GenerationStatus.Error, error = result.error)
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            _uiState.update { it.copy(generationStatus = GenerationStatus.Error, error = e.message) }
        }
    }

    private suspend fun pollForResult(promptId: String) {
        var attempts = 0
        while (attempts < MAX_POLL_ATTEMPTS) {
            delay(POLL_INTERVAL_MS)
            try {
                val result = pollResult(promptId)
                when (result.status) {
                    GenerationStatus.Completed -> {
                        _uiState.update {
                            it.copy(generationStatus = GenerationStatus.Completed, result = result)
                        }
                        return
                    }
                    GenerationStatus.Error -> {
                        _uiState.update {
                            it.copy(generationStatus = GenerationStatus.Error, error = result.error)
                        }
                        return
                    }
                    else -> attempts++
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                _uiState.update {
                    it.copy(generationStatus = GenerationStatus.Error, error = e.message)
                }
                return
            }
        }
        _uiState.update {
            it.copy(generationStatus = GenerationStatus.Error, error = "Generation timed out")
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 3000L
        private const val MAX_POLL_ATTEMPTS = 120
    }
}
