package com.riox432.civitdeck.feature.comfyui.presentation

import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.DiffusionModelFamily
import com.riox432.civitdeck.domain.model.DiffusionModelSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Which server folder the selected model comes from, and so which loader opens it. */
enum class GenerationModelSource {
    CHECKPOINT,
    DIFFUSION_MODEL,
}

/**
 * Model selection for the generation form: checkpoint or diffusion model, and for a diffusion
 * model its family, text encoder and VAE. Extracted from [ComfyUIGenerationViewModel] to
 * reduce function count.
 *
 * The form cannot show the sampler and scheduler, so they follow the family: picking a family
 * applies them with its other defaults, and picking a different model resets them, because the
 * family is not guessable from a file name and no longer applies.
 */
internal class GenerationModelSelector(
    private val uiState: MutableStateFlow<GenerationUiState>,
) {

    fun selectCheckpoint(checkpoint: String) {
        uiState.update { it.withCheckpoint(checkpoint) }
    }

    /**
     * Also switches ControlNet off and clears the inpainting images: the built-in workflow has
     * no ControlNet or inpainting graph for a diffusion model and rejects either setting.
     */
    fun selectDiffusionModel(model: String) {
        uiState.update { state ->
            val selected = state.withDiffusionModel(model)
            val hasInpaintingImage = selected.initImageFilename != null || selected.maskImageFilename != null
            if (selected.controlNetEnabled || hasInpaintingImage) {
                selected.copy(controlNetEnabled = false, initImageFilename = null, maskImageFilename = null)
            } else {
                selected
            }
        }
    }

    /**
     * Applies [family]'s defaults and preselects the text encoder and VAE its hints match,
     * keeping the current file when none matches. Re-selecting the current family keeps the
     * user's edits.
     */
    fun selectFamily(family: DiffusionModelFamily) {
        uiState.update { state ->
            if (state.selectedFamily == family) {
                state
            } else {
                state.withFamily(family).copy(
                    steps = family.steps,
                    cfgScale = family.cfgScale,
                    width = family.width,
                    height = family.height,
                )
            }
        }
    }

    /**
     * Selects the server file matching [fileName] for a prefill, from the list its folder
     * decides, and sets [family]. Steps, CFG and size are left alone: the prefill has already
     * set them, with the family's values only where the CivitAI metadata had none.
     *
     * With no family, a file in both lists counts as a checkpoint and a file in neither falls
     * back to the first checkpoint, as before families existed. With a family, a file in both
     * lists counts as a diffusion model, and a file in neither selects nothing, because the
     * family's defaults would be wrong for an arbitrary checkpoint.
     */
    fun selectPrefilledModel(fileName: String, family: DiffusionModelFamily?) {
        uiState.update { state ->
            val checkpoint = findModelFile(state.checkpoints, fileName)
            val diffusionModel = findModelFile(state.diffusionModels, fileName)
            if (family == null) {
                when {
                    checkpoint != null -> state.withCheckpoint(checkpoint)
                    diffusionModel != null -> state.withDiffusionModel(diffusionModel)
                    else -> state.withCheckpoint(state.checkpoints.firstOrNull().orEmpty())
                }
            } else {
                val selected = when {
                    diffusionModel != null -> state.withDiffusionModel(diffusionModel)
                    checkpoint != null -> state.withCheckpoint(checkpoint)
                    else -> state.withCheckpoint("").copy(selectedDiffusionModel = "")
                }
                if (selected.selectedFamily == family) selected else selected.withFamily(family)
            }
        }
    }

    fun selectTextEncoder(textEncoder: String) {
        uiState.update { it.copy(selectedTextEncoder = textEncoder) }
    }

    fun selectVae(vae: String) {
        uiState.update { it.copy(selectedVae = vae) }
    }

    private fun GenerationUiState.withCheckpoint(checkpoint: String) =
        if (modelSource == GenerationModelSource.CHECKPOINT && selectedCheckpoint == checkpoint) {
            this
        } else {
            withoutFamily().copy(modelSource = GenerationModelSource.CHECKPOINT, selectedCheckpoint = checkpoint)
        }

    private fun GenerationUiState.withDiffusionModel(model: String) =
        if (modelSource == GenerationModelSource.DIFFUSION_MODEL && selectedDiffusionModel == model) {
            this
        } else {
            withoutFamily().copy(modelSource = GenerationModelSource.DIFFUSION_MODEL, selectedDiffusionModel = model)
        }

    private fun GenerationUiState.withFamily(family: DiffusionModelFamily) = copy(
        selectedFamily = family,
        samplerName = family.samplerName,
        scheduler = family.scheduler,
        selectedTextEncoder = textEncoders.firstWithPrefix(family.textEncoderHint) ?: selectedTextEncoder,
        selectedVae = vaes.firstWithPrefix(family.vaeHint) ?: selectedVae,
    )

    // Visible fields (steps, CFG, size) stay as the user left them.
    private fun GenerationUiState.withoutFamily() = copy(
        selectedFamily = null,
        samplerName = ComfyUIGenerationParams.DEFAULT_SAMPLER,
        scheduler = ComfyUIGenerationParams.DEFAULT_SCHEDULER,
    )

    private fun List<String>.firstWithPrefix(hint: String): String? =
        firstOrNull { modelFileName(it).startsWith(hint, ignoreCase = true) }
}

/**
 * ComfyUI lists model files with their subfolder (`SDXL/foo.safetensors`), while CivitAI
 * metadata and templates carry a bare file name, so a full-path match is tried first and
 * then the file name alone, ignoring case.
 */
internal fun findModelFile(available: List<String>, requested: String): String? {
    available.firstOrNull { it.equals(requested, ignoreCase = true) }?.let { return it }
    val requestedName = modelFileName(requested)
    return available.firstOrNull { modelFileName(it).equals(requestedName, ignoreCase = true) }
}

internal fun modelFileName(path: String): String =
    path.substringAfterLast('/').substringAfterLast('\\')

/** The split-loader files to submit, or null when the form generates from a checkpoint. */
internal fun GenerationUiState.diffusionModelSelection(): DiffusionModelSelection? {
    val family = selectedFamily
    if (modelSource != GenerationModelSource.DIFFUSION_MODEL || family == null) return null
    return DiffusionModelSelection(
        unetName = selectedDiffusionModel,
        textEncoderName = selectedTextEncoder,
        clipType = family.clipType,
        vaeName = selectedVae,
        latentNode = family.latentNode,
        auraFlowShift = family.auraFlowShift,
    )
}
