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
        uiState.update { state ->
            if (state.modelSource == GenerationModelSource.CHECKPOINT && state.selectedCheckpoint == checkpoint) {
                state
            } else {
                state.withoutFamily().copy(
                    modelSource = GenerationModelSource.CHECKPOINT,
                    selectedCheckpoint = checkpoint,
                )
            }
        }
    }

    fun selectDiffusionModel(model: String) {
        uiState.update { state ->
            if (state.modelSource == GenerationModelSource.DIFFUSION_MODEL && state.selectedDiffusionModel == model) {
                state
            } else {
                state.withoutFamily().copy(
                    modelSource = GenerationModelSource.DIFFUSION_MODEL,
                    selectedDiffusionModel = model,
                )
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
                state.copy(
                    selectedFamily = family,
                    steps = family.steps,
                    cfgScale = family.cfgScale,
                    samplerName = family.samplerName,
                    scheduler = family.scheduler,
                    width = family.width,
                    height = family.height,
                    selectedTextEncoder = state.textEncoders.firstWithPrefix(family.textEncoderHint)
                        ?: state.selectedTextEncoder,
                    selectedVae = state.vaes.firstWithPrefix(family.vaeHint) ?: state.selectedVae,
                )
            }
        }
    }

    fun selectTextEncoder(textEncoder: String) {
        uiState.update { it.copy(selectedTextEncoder = textEncoder) }
    }

    fun selectVae(vae: String) {
        uiState.update { it.copy(selectedVae = vae) }
    }

    // Visible fields (steps, CFG, size) stay as the user left them.
    private fun GenerationUiState.withoutFamily() = copy(
        selectedFamily = null,
        samplerName = ComfyUIGenerationParams.DEFAULT_SAMPLER,
        scheduler = ComfyUIGenerationParams.DEFAULT_SCHEDULER,
    )

    private fun List<String>.firstWithPrefix(hint: String): String? =
        firstOrNull { modelFileName(it).startsWith(hint, ignoreCase = true) }
}

/** The split-loader files to submit, or null when the form generates from a checkpoint. */
internal fun GenerationUiState.diffusionModelSelection(): DiffusionModelSelection? {
    val family = selectedFamily
    if (modelSource != GenerationModelSource.DIFFUSION_MODEL || family == null) return null
    return DiffusionModelSelection(
        unetName = selectedDiffusionModel,
        textEncoderName = selectedTextEncoder,
        clipType = family.clipType,
        vaeName = selectedVae,
    )
}
