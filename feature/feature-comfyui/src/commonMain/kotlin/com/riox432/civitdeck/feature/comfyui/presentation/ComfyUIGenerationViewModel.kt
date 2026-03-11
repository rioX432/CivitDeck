package com.riox432.civitdeck.feature.comfyui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.data.image.SaveGeneratedImageUseCase
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.GenerationResult
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.model.LoraSelection
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUICheckpointsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUIControlNetsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUILorasUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ImportWorkflowUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveGenerationProgressUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.PollComfyUIResultUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SubmitComfyUIGenerationUseCase
import com.riox432.civitdeck.util.Logger
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
    // LoRA
    val availableLoras: List<String> = emptyList(),
    val isLoadingLoras: Boolean = false,
    val loraSelections: List<LoraSelection> = emptyList(),
    // ControlNet
    val availableControlNets: List<String> = emptyList(),
    val isLoadingControlNets: Boolean = false,
    val controlNetEnabled: Boolean = false,
    val selectedControlNet: String = "",
    val controlNetStrength: Float = 1.0f,
    // Custom workflow
    val customWorkflowJson: String? = null,
    val workflowImportError: String? = null,
    // Generation
    val generationStatus: GenerationStatus = GenerationStatus.Idle,
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val result: GenerationResult? = null,
    val error: String? = null,
    val imageSaveSuccess: Boolean? = null,
) {
    /** Progress fraction in [0f, 1f]. Returns 0 when totalSteps is unknown. */
    val progressFraction: Float
        get() = if (totalSteps > 0) currentStep.toFloat() / totalSteps.toFloat() else 0f
}

@Suppress("TooManyFunctions")
class ComfyUIGenerationViewModel(
    private val fetchCheckpoints: FetchComfyUICheckpointsUseCase,
    private val fetchLoras: FetchComfyUILorasUseCase,
    private val fetchControlNets: FetchComfyUIControlNetsUseCase,
    private val importWorkflow: ImportWorkflowUseCase,
    private val submitGeneration: SubmitComfyUIGenerationUseCase,
    private val pollResult: PollComfyUIResultUseCase,
    private val observeProgress: ObserveGenerationProgressUseCase,
    private val saveImage: SaveGeneratedImageUseCase,
    private val repository: ComfyUIConnectionRepository,
) : ViewModel() {

    private var progressJob: Job? = null

    private val _uiState = MutableStateFlow(GenerationUiState())
    val uiState: StateFlow<GenerationUiState> = _uiState

    init {
        loadCheckpoints()
        loadLoras()
        loadControlNets()
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

    private fun loadLoras() {
        _uiState.update { it.copy(isLoadingLoras = true) }
        viewModelScope.launch {
            try {
                val list = fetchLoras()
                _uiState.update { it.copy(availableLoras = list, isLoadingLoras = false) }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w("ComfyUIGeneration", "Failed to fetch loras: ${e.message}")
                _uiState.update { it.copy(isLoadingLoras = false) }
            }
        }
    }

    private fun loadControlNets() {
        _uiState.update { it.copy(isLoadingControlNets = true) }
        viewModelScope.launch {
            try {
                val list = fetchControlNets()
                _uiState.update { it.copy(availableControlNets = list, isLoadingControlNets = false) }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w("ComfyUIGeneration", "Failed to fetch control nets: ${e.message}")
                _uiState.update { it.copy(isLoadingControlNets = false) }
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

    // -- LoRA --

    fun onLoraAdded(loraName: String) {
        val current = _uiState.value.loraSelections
        if (current.none { it.name == loraName }) {
            _uiState.update { it.copy(loraSelections = current + LoraSelection(loraName)) }
        }
    }

    fun onLoraRemoved(loraName: String) {
        _uiState.update { state ->
            state.copy(loraSelections = state.loraSelections.filter { it.name != loraName })
        }
    }

    fun onLoraStrengthChanged(loraName: String, strengthModel: Float, strengthClip: Float) {
        _uiState.update { state ->
            state.copy(
                loraSelections = state.loraSelections.map {
                    if (it.name == loraName) {
                        it.copy(strengthModel = strengthModel, strengthClip = strengthClip)
                    } else {
                        it
                    }
                }
            )
        }
    }

    // -- ControlNet --

    fun onControlNetToggled(enabled: Boolean) {
        _uiState.update { it.copy(controlNetEnabled = enabled) }
    }

    fun onControlNetSelected(model: String) {
        _uiState.update { it.copy(selectedControlNet = model) }
    }

    fun onControlNetStrengthChanged(strength: Float) {
        _uiState.update { it.copy(controlNetStrength = strength) }
    }

    // -- Custom workflow --

    fun onImportWorkflow(jsonInput: String) {
        try {
            val validated = importWorkflow(jsonInput)
            _uiState.update { it.copy(customWorkflowJson = validated, workflowImportError = null) }
        } catch (e: IllegalStateException) {
            _uiState.update { it.copy(workflowImportError = e.message) }
        }
    }

    fun onClearCustomWorkflow() {
        _uiState.update { it.copy(customWorkflowJson = null, workflowImportError = null) }
    }

    fun onGenerate() {
        val state = _uiState.value
        val hasCustomWorkflow = state.customWorkflowJson != null
        if (!hasCustomWorkflow && (state.selectedCheckpoint.isBlank() || state.prompt.isBlank())) return
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

    fun onSaveImage(imageUrl: String) {
        viewModelScope.launch {
            val success = saveImage(imageUrl)
            _uiState.update { it.copy(imageSaveSuccess = success) }
        }
    }

    fun onDismissSaveResult() {
        _uiState.update { it.copy(imageSaveSuccess = null) }
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
        loraSelections = state.loraSelections,
        controlNetEnabled = state.controlNetEnabled,
        controlNetModel = state.selectedControlNet,
        controlNetStrength = state.controlNetStrength,
        customWorkflowJson = state.customWorkflowJson,
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
