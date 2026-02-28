package com.riox432.civitdeck.feature.comfyui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.GenerationResult
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUICheckpointsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.PollComfyUIResultUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SubmitComfyUIGenerationUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val result: GenerationResult? = null,
    val error: String? = null,
)

class ComfyUIGenerationViewModel(
    private val fetchCheckpoints: FetchComfyUICheckpointsUseCase,
    private val submitGeneration: SubmitComfyUIGenerationUseCase,
    private val pollResult: PollComfyUIResultUseCase,
) : ViewModel() {

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
        _uiState.update {
            it.copy(generationStatus = GenerationStatus.Submitting, error = null, result = null)
        }

        viewModelScope.launch {
            try {
                val params = ComfyUIGenerationParams(
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
                val promptId = submitGeneration(params)
                _uiState.update { it.copy(generationStatus = GenerationStatus.Running) }
                pollForResult(promptId)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                _uiState.update {
                    it.copy(generationStatus = GenerationStatus.Error, error = e.message)
                }
            }
        }
    }

    private suspend fun pollForResult(promptId: String) {
        var attempts = 0
        val maxAttempts = 120
        while (attempts < maxAttempts) {
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
                            it.copy(
                                generationStatus = GenerationStatus.Error,
                                error = result.error,
                            )
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
    }
}
