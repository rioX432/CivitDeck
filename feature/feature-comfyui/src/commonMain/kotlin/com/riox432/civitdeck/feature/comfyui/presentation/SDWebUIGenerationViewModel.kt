package com.riox432.civitdeck.feature.comfyui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.SDWebUIGenerationParams
import com.riox432.civitdeck.domain.model.SDWebUIGenerationProgress
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchSDWebUIModelsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchSDWebUISamplersUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchSDWebUIVaesUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.GenerateSDWebUIImageUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.InterruptSDWebUIGenerationUseCase
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SDWebUIGenerationUiState(
    val models: List<String> = emptyList(),
    val samplers: List<String> = emptyList(),
    val vaes: List<String> = emptyList(),
    val selectedModel: String = "",
    val selectedSampler: String = "Euler",
    val selectedVae: String = "",
    val prompt: String = "",
    val negativePrompt: String = "",
    val steps: Int = 20,
    val cfgScale: Double = 7.0,
    val width: Int = 512,
    val height: Int = 512,
    val seed: Long = -1,
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val progress: Double = 0.0,
    val progressStep: Int = 0,
    val progressTotalSteps: Int = 0,
    val generatedImages: List<String> = emptyList(),
    val error: String? = null,
)

class SDWebUIGenerationViewModel(
    private val fetchModels: FetchSDWebUIModelsUseCase,
    private val fetchSamplers: FetchSDWebUISamplersUseCase,
    private val fetchVaes: FetchSDWebUIVaesUseCase,
    private val generateImage: GenerateSDWebUIImageUseCase,
    private val interruptGeneration: InterruptSDWebUIGenerationUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SDWebUIGenerationUiState())
    val uiState: StateFlow<SDWebUIGenerationUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null

    init { loadResources() }

    private fun loadResources() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val models = suspendRunCatching { fetchModels() }.getOrDefault(emptyList())
                val samplers = suspendRunCatching { fetchSamplers() }.getOrDefault(emptyList())
                val vaes = suspendRunCatching { fetchVaes() }.getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        models = models,
                        samplers = samplers,
                        vaes = vaes,
                        selectedModel = models.firstOrNull() ?: "",
                        selectedSampler = samplers.firstOrNull() ?: "Euler",
                    )
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.e(TAG, "Failed to load resources: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onPromptChanged(value: String) { _uiState.update { it.copy(prompt = value) } }
    fun onNegativePromptChanged(value: String) { _uiState.update { it.copy(negativePrompt = value) } }
    fun onModelSelected(model: String) { _uiState.update { it.copy(selectedModel = model) } }
    fun onSamplerSelected(sampler: String) { _uiState.update { it.copy(selectedSampler = sampler) } }
    fun onStepsChanged(steps: Int) { _uiState.update { it.copy(steps = steps) } }
    fun onCfgChanged(cfg: Double) { _uiState.update { it.copy(cfgScale = cfg) } }
    fun onWidthChanged(w: Int) { _uiState.update { it.copy(width = w) } }
    fun onHeightChanged(h: Int) { _uiState.update { it.copy(height = h) } }
    fun onSeedChanged(seed: Long) { _uiState.update { it.copy(seed = seed) } }
    fun onDismissError() { _uiState.update { it.copy(error = null) } }

    fun onGenerate() {
        val state = _uiState.value
        if (state.isGenerating || state.prompt.isBlank()) return
        _uiState.update {
            it.copy(isGenerating = true, generatedImages = emptyList(), error = null, progress = 0.0)
        }
        val params = SDWebUIGenerationParams(
            prompt = state.prompt,
            negativePrompt = state.negativePrompt,
            steps = state.steps,
            cfgScale = state.cfgScale,
            width = state.width,
            height = state.height,
            samplerName = state.selectedSampler,
            seed = state.seed,
        )
        generationJob = viewModelScope.launch {
            generateImage(params).collect { progress ->
                handleProgress(progress)
            }
        }
    }

    private fun handleProgress(progress: SDWebUIGenerationProgress) {
        when (progress) {
            is SDWebUIGenerationProgress.Generating -> _uiState.update {
                it.copy(
                    progress = progress.fraction,
                    progressStep = progress.step,
                    progressTotalSteps = progress.totalSteps,
                )
            }
            is SDWebUIGenerationProgress.Completed -> _uiState.update {
                it.copy(isGenerating = false, generatedImages = progress.base64Images, progress = 1.0)
            }
            is SDWebUIGenerationProgress.Error -> _uiState.update {
                it.copy(isGenerating = false, error = progress.message)
            }
        }
    }

    fun onInterrupt() {
        generationJob?.cancel()
        viewModelScope.launch {
            suspendRunCatching { interruptGeneration() }
                .onFailure { e -> Logger.w(TAG, "Interrupt failed: ${e.message}") }
            _uiState.update { it.copy(isGenerating = false) }
        }
    }

    companion object {
        private const val TAG = "SDWebUIGenerationVM"
    }
}
