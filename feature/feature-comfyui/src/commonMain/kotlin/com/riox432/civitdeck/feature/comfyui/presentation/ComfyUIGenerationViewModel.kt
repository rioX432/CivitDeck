package com.riox432.civitdeck.feature.comfyui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.data.image.SaveGeneratedImageUseCase
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.GenerationResult
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.model.LoraSelection
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
import com.riox432.civitdeck.feature.comfyui.domain.model.ExtractedParameter
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ExtractWorkflowParametersUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUICheckpointsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUIControlNetsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUILorasUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchObjectInfoUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ImportWorkflowUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.InjectWorkflowParametersUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.InterruptComfyUIGenerationUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveGenerationProgressUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.PollComfyUIResultUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SubmitComfyUIGenerationUseCase
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.CancellationException
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
    // Dynamic workflow parameters
    val extractedParameters: List<ExtractedParameter> = emptyList(),
    val isLoadingParameters: Boolean = false,
    // Inpainting mask
    val initImageFilename: String? = null,
    val maskImageFilename: String? = null,
    val denoiseStrength: Double = 0.75,
    // Generation
    val generationStatus: GenerationStatus = GenerationStatus.Idle,
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val result: GenerationResult? = null,
    val error: String? = null,
    val imageSaveSuccess: Boolean? = null,
    /** Latest preview image bytes from WebSocket during generation. */
    val previewImageBytes: ByteArray? = null,
    val currentNodeName: String = "",
) {
    /** Progress fraction in [0f, 1f]. Returns 0 when totalSteps is unknown. */
    val progressFraction: Float
        get() = if (totalSteps > 0) currentStep.toFloat() / totalSteps.toFloat() else 0f
}

@Suppress("LongParameterList", "TooManyFunctions")
class ComfyUIGenerationViewModel(
    private val fetchCheckpoints: FetchComfyUICheckpointsUseCase,
    private val fetchLoras: FetchComfyUILorasUseCase,
    private val fetchControlNets: FetchComfyUIControlNetsUseCase,
    private val importWorkflow: ImportWorkflowUseCase,
    private val extractParameters: ExtractWorkflowParametersUseCase,
    private val injectParameters: InjectWorkflowParametersUseCase,
    private val fetchObjectInfo: FetchObjectInfoUseCase,
    submitGeneration: SubmitComfyUIGenerationUseCase,
    pollResult: PollComfyUIResultUseCase,
    observeProgress: ObserveGenerationProgressUseCase,
    interruptGeneration: InterruptComfyUIGenerationUseCase,
    saveImage: SaveGeneratedImageUseCase,
    repository: ComfyUIConnectionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenerationUiState())
    val uiState: StateFlow<GenerationUiState> = _uiState

    private val generationDelegate = GenerationExecutionDelegate(
        scope = viewModelScope,
        uiState = _uiState,
        submitGeneration = submitGeneration,
        pollResult = pollResult,
        observeProgress = observeProgress,
        interruptGeneration = interruptGeneration,
        saveImage = saveImage,
        repository = repository,
    )

    init {
        loadCheckpoints()
        loadLoras()
        loadControlNets()
    }

    private fun loadCheckpoints() {
        _uiState.update { it.copy(isLoadingCheckpoints = true) }
        launchWithErrorHandling(
            tag = "Failed to load checkpoints",
            onError = { e -> _uiState.update { it.copy(isLoadingCheckpoints = false, error = e.message) } },
        ) {
            val list = fetchCheckpoints()
            _uiState.update {
                it.copy(
                    checkpoints = list,
                    selectedCheckpoint = list.firstOrNull() ?: "",
                    isLoadingCheckpoints = false,
                )
            }
        }
    }

    private fun loadLoras() {
        _uiState.update { it.copy(isLoadingLoras = true) }
        launchWithErrorHandling(
            tag = "Failed to fetch loras",
            onError = { _uiState.update { it.copy(isLoadingLoras = false) } },
        ) {
            val list = fetchLoras()
            _uiState.update { it.copy(availableLoras = list, isLoadingLoras = false) }
        }
    }

    private fun loadControlNets() {
        _uiState.update { it.copy(isLoadingControlNets = true) }
        launchWithErrorHandling(
            tag = "Failed to fetch control nets",
            onError = { _uiState.update { it.copy(isLoadingControlNets = false) } },
        ) {
            val list = fetchControlNets()
            _uiState.update { it.copy(availableControlNets = list, isLoadingControlNets = false) }
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
            extractWorkflowParameters(validated)
        } catch (e: IllegalStateException) {
            _uiState.update { it.copy(workflowImportError = e.message) }
        }
    }

    fun onClearCustomWorkflow() {
        _uiState.update {
            it.copy(
                customWorkflowJson = null,
                workflowImportError = null,
                extractedParameters = emptyList(),
            )
        }
    }

    // -- Dynamic workflow parameters --

    fun onParameterValueChanged(nodeId: String, paramName: String, newValue: String) {
        _uiState.update { state ->
            state.copy(
                extractedParameters = state.extractedParameters.map { param ->
                    if (param.nodeId == nodeId && param.paramName == paramName) {
                        param.copy(currentValue = newValue)
                    } else {
                        param
                    }
                }
            )
        }
    }

    fun onRefreshParameters() {
        val json = _uiState.value.customWorkflowJson ?: return
        extractWorkflowParameters(json)
    }

    private fun extractWorkflowParameters(workflowJson: String) {
        _uiState.update { it.copy(isLoadingParameters = true) }
        launchWithErrorHandling(
            tag = "Failed to extract workflow parameters",
            onError = { _uiState.update { it.copy(isLoadingParameters = false) } },
        ) {
            val objectInfoJson = fetchObjectInfo()
            val params = extractParameters(workflowJson, objectInfoJson)
            _uiState.update { it.copy(extractedParameters = params, isLoadingParameters = false) }
        }
    }

    // -- Inpainting mask --

    fun onInitImageUploaded(filename: String) {
        _uiState.update { it.copy(initImageFilename = filename) }
    }

    fun onMaskUploaded(filename: String) {
        _uiState.update { it.copy(maskImageFilename = filename) }
    }

    fun onClearMask() {
        _uiState.update {
            it.copy(initImageFilename = null, maskImageFilename = null)
        }
    }

    fun onDenoiseStrengthChanged(strength: Double) {
        _uiState.update { it.copy(denoiseStrength = strength) }
    }

    // -- Generation (delegated) --

    fun onGenerate() {
        val state = _uiState.value
        val hasCustomWorkflow = state.customWorkflowJson != null
        if (!hasCustomWorkflow && (state.selectedCheckpoint.isBlank() || state.prompt.isBlank())) return
        generationDelegate.onGenerate(buildParams(state))
    }

    fun onSaveImage(imageUrl: String) = generationDelegate.onSaveImage(imageUrl)

    fun onDismissSaveResult() = generationDelegate.onDismissSaveResult()

    fun onInterrupt() = generationDelegate.onInterrupt()

    private fun buildParams(state: GenerationUiState): ComfyUIGenerationParams {
        // If custom workflow has extracted parameters, inject modified values before submission
        val finalWorkflowJson = if (state.customWorkflowJson != null && state.extractedParameters.isNotEmpty()) {
            injectParameters(state.customWorkflowJson, state.extractedParameters)
        } else {
            state.customWorkflowJson
        }

        return ComfyUIGenerationParams(
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
            customWorkflowJson = finalWorkflowJson,
            initImageFilename = state.initImageFilename,
            maskImageFilename = state.maskImageFilename,
            denoiseStrength = state.denoiseStrength,
        )
    }

    private inline fun launchWithErrorHandling(
        tag: String,
        crossinline onError: (Exception) -> Unit,
        crossinline block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.e(TAG, "$tag: ${e.message}")
                onError(e)
            }
        }
    }

    companion object {
        private const val TAG = "ComfyUIGenerationVM"
    }
}
