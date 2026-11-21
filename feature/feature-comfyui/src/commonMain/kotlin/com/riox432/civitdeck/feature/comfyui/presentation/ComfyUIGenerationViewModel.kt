package com.riox432.civitdeck.feature.comfyui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.ComfyUIJobTarget
import com.riox432.civitdeck.domain.model.DiffusionModelFamily
import com.riox432.civitdeck.domain.model.GenerationResult
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.model.LoraSelection
import com.riox432.civitdeck.domain.model.SourceGenerationSnapshot
import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.feature.comfyui.domain.model.ExtractedParameter
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ApplyWorkflowTemplateUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ImportWorkflowUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.InjectWorkflowParametersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

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
    // Split-loader model files (UNETLoader / CLIPLoader / VAELoader)
    val diffusionModels: List<String> = emptyList(),
    val textEncoders: List<String> = emptyList(),
    val vaes: List<String> = emptyList(),
    val serverClipTypes: List<String> = emptyList(),
    // Model selection (GenerationModelSelector)
    val modelSource: GenerationModelSource = GenerationModelSource.CHECKPOINT,
    val selectedDiffusionModel: String = "",
    val selectedFamily: DiffusionModelFamily? = null,
    val selectedTextEncoder: String = "",
    val selectedVae: String = "",
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

    /** Whether Generate can submit; platforms use this instead of their own checks. */
    val canGenerate: Boolean
        get() = customWorkflowJson != null || (prompt.isNotBlank() && hasCompleteModelSelection)

    private val hasCompleteModelSelection: Boolean
        get() = when (modelSource) {
            GenerationModelSource.CHECKPOINT -> selectedCheckpoint.isNotBlank()
            GenerationModelSource.DIFFUSION_MODEL ->
                selectedDiffusionModel.isNotBlank() &&
                    selectedFamily?.isSupportedBy(serverClipTypes) == true &&
                    selectedTextEncoder.isNotBlank() &&
                    selectedVae.isNotBlank()
        }
}

// Android and iOS call every form action on the VM directly, so model selection keeps thin
// forwarding functions here instead of exposing GenerationModelSelector as a second call style.
@Suppress("TooManyFunctions")
class ComfyUIGenerationViewModel(
    executionUseCases: GenerationExecutionUseCases,
    resourceUseCases: GenerationResourceUseCases,
    private val importWorkflow: ImportWorkflowUseCase,
    private val injectParameters: InjectWorkflowParametersUseCase,
    private val applyTemplate: ApplyWorkflowTemplateUseCase,
    referenceTrialDependencies: ReferenceTrialDependencies? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenerationUiState())
    val uiState: StateFlow<GenerationUiState> = _uiState

    private val generationDelegate = GenerationExecutionDelegate(
        scope = viewModelScope,
        uiState = _uiState,
        useCases = executionUseCases,
    )

    private val modelSelector = GenerationModelSelector(_uiState)

    private val resourceLoader = GenerationResourceLoader(
        scope = viewModelScope,
        uiState = _uiState,
        useCases = resourceUseCases,
        modelSelector = modelSelector,
    )

    private val referenceTrial = ReferenceTrialController(
        scope = viewModelScope,
        uiState = _uiState,
        dependencies = referenceTrialDependencies,
        connections = executionUseCases.repository,
        generationInputs = ::buildParams,
        submitPrepared = { generationDelegate.onGenerate(it) },
    )

    /** Non-null while a saved reference is linked; Generate then goes through its review. */
    val referenceReviewState: StateFlow<ReferenceReviewState?> = referenceTrial.state

    init {
        resourceLoader.loadCheckpoints()
        resourceLoader.loadLoras()
        resourceLoader.loadControlNets()
        resourceLoader.loadDiffusionModelResources()
    }

    fun onCheckpointSelected(checkpoint: String) = modelSelector.selectCheckpoint(checkpoint)

    fun onDiffusionModelSelected(model: String) = modelSelector.selectDiffusionModel(model)

    fun onModelFamilySelected(family: DiffusionModelFamily) = modelSelector.selectFamily(family)

    fun onTextEncoderSelected(textEncoder: String) = modelSelector.selectTextEncoder(textEncoder)

    fun onVaeSelected(vae: String) = modelSelector.selectVae(vae)

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

    // Swift does not see Kotlin default arguments (SKIE's default-argument interop is off), so
    // the iOS call site without a base model needs this overload.
    fun applyPrefill(params: ComfyUIGenerationParams) = applyPrefill(params, baseModel = null)

    /**
     * Fills the form from [params]. A blank prompt or negative prompt keeps what the user
     * typed. Sampler and scheduler in [params] are not applied: producers emit A1111-style
     * names that ComfyUI rejects, and the form has no control to correct them.
     *
     * [baseModel] is CivitAI's base model of the prefilled file. When it names a
     * [DiffusionModelFamily], that family is selected with its sampler and scheduler, and the
     * file is looked up among diffusion models as well as checkpoints.
     */
    fun applyPrefill(params: ComfyUIGenerationParams, baseModel: String?) {
        _uiState.update { state ->
            state.copy(
                prompt = params.prompt.ifBlank { state.prompt },
                negativePrompt = params.negativePrompt.ifBlank { state.negativePrompt },
                steps = params.steps,
                cfgScale = params.cfgScale,
                seed = params.seed,
                width = params.width,
                height = params.height,
            )
        }
        val family = DiffusionModelFamily.forBaseModel(baseModel)
        if (family != null || params.checkpoint.isNotBlank()) {
            resourceLoader.requestModel(params.checkpoint, family)
        }
    }

    /**
     * Loads [template] into the form. A template imported from a raw ComfyUI workflow is
     * loaded as a custom workflow, and [values] are ignored because its variables are node
     * parameters edited after import. Any other template clears a loaded custom workflow,
     * since that workflow would otherwise be submitted instead of the form values.
     */
    fun onTemplateApplied(template: WorkflowTemplate, values: Map<String, String> = emptyMap()) {
        val rawWorkflowJson = template.rawWorkflowJson
        if (rawWorkflowJson != null) {
            onImportWorkflow(rawWorkflowJson)
        } else {
            onClearCustomWorkflow()
            applyPrefill(applyTemplate(template, values))
        }
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
            resourceLoader.extractWorkflowParameters(validated)
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
        resourceLoader.extractWorkflowParameters(json)
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
        if (!state.canGenerate || referenceTrial.isLinked) return
        generationDelegate.onGenerate(buildParams(state))
    }

    /** Generates on [target] even while another connection is active; never substitutes another server. */
    fun onGenerate(target: ComfyUIJobTarget) {
        val state = _uiState.value
        if (!state.canGenerate || referenceTrial.isLinked) return
        generationDelegate.onGenerate(buildParams(state), target)
    }

    fun onSaveImage(imageUrl: String) = generationDelegate.onSaveImage(imageUrl)

    fun onDismissSaveResult() = generationDelegate.onDismissSaveResult()

    fun onInterrupt() = generationDelegate.onInterrupt()

    // -- Reference trial --

    /**
     * Links saved reference [referenceId] to the form. Everything the previous draft set that a
     * reference does not carry (custom workflow, inpainting, LoRAs, ControlNet) is cleared, and
     * only values the source recorded are filled in.
     */
    fun beginReferenceTrial(referenceId: Long) =
        referenceTrial.begin(referenceId, ::resetDraftForReference, ::applySourceValues)

    /** Prepares the exact payload for the active connection and reviews it; Generate calls this while linked. */
    fun prepareReferenceReview() = referenceTrial.prepare()

    fun onReferenceVariationAcknowledged(acknowledged: Boolean) = referenceTrial.acknowledgeVariation(acknowledged)

    fun confirmReferenceReview() = referenceTrial.confirm()

    /** Closes the review and keeps the reference linked, so Generate still needs a review. */
    fun dismissReferenceReview() = referenceTrial.dismiss()

    /** Unlinks the reference; the form generates manually again. */
    fun clearReferenceTrial() = referenceTrial.clear()

    private fun applySourceValues(gen: SourceGenerationSnapshot) {
        _uiState.update { it.withSourceValues(gen) }
        gen.model?.takeIf { it.isNotBlank() }?.let { resourceLoader.requestModel(it, family = null) }
    }

    private fun resetDraftForReference() {
        resourceLoader.cancelPendingWork()
        val blank = GenerationUiState()
        // Server choice lists and the model selection stay; the model selection carries the
        // family's sampler and scheduler.
        _uiState.update {
            it.copy(
                prompt = blank.prompt,
                negativePrompt = blank.negativePrompt,
                steps = blank.steps,
                cfgScale = blank.cfgScale,
                width = blank.width,
                height = blank.height,
                seed = blank.seed,
                loraSelections = blank.loraSelections,
                controlNetEnabled = blank.controlNetEnabled,
                selectedControlNet = blank.selectedControlNet,
                controlNetStrength = blank.controlNetStrength,
                customWorkflowJson = blank.customWorkflowJson,
                workflowImportError = blank.workflowImportError,
                extractedParameters = blank.extractedParameters,
                isLoadingParameters = false,
                initImageFilename = blank.initImageFilename,
                maskImageFilename = blank.maskImageFilename,
                denoiseStrength = blank.denoiseStrength,
                error = null,
            )
        }
    }

    private fun buildParams(state: GenerationUiState): ComfyUIGenerationParams {
        // If custom workflow has extracted parameters, inject modified values before submission
        val finalWorkflowJson = if (state.customWorkflowJson != null && state.extractedParameters.isNotEmpty()) {
            injectParameters(state.customWorkflowJson, state.extractedParameters)
        } else {
            state.customWorkflowJson
        }

        val diffusionModel = state.diffusionModelSelection()
        return ComfyUIGenerationParams(
            checkpoint = if (diffusionModel == null) state.selectedCheckpoint else "",
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
            diffusionModel = diffusionModel,
        )
    }
}
