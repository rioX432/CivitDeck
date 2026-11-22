package com.riox432.civitdeck.domain.model

/**
 * A reference generation fixed before submission: what the user reviews is what is sent.
 *
 * [workflowJson] is the prompt graph serialized once, without the request's `client_id`
 * wrapper; submission sends it as stored instead of rebuilding it. [effectiveParams] and
 * [effectiveSettings] carry the concrete seed written into that graph, never the random
 * sentinel. [target] is the server the graph will be sent to.
 */
data class PreparedReferenceGeneration(
    val referenceId: Long,
    val target: ComfyUIJobTarget,
    val effectiveParams: ComfyUIGenerationParams,
    val workflowJson: String,
    val effectiveSettings: EffectiveGenerationSettings,
)

/** The settings a prepared graph runs with, for review before submission. */
data class EffectiveGenerationSettings(
    val checkpoint: String,
    val diffusionModel: DiffusionModelSelection?,
    val prompt: String,
    val negativePrompt: String,
    val steps: Int,
    val cfgScale: Double,
    val seed: Long,
    val width: Int,
    val height: Int,
    val samplerName: String,
    val scheduler: String,
    val loraSelections: List<LoraSelection>,
) {
    companion object {
        fun from(params: ComfyUIGenerationParams): EffectiveGenerationSettings = EffectiveGenerationSettings(
            checkpoint = params.checkpoint,
            diffusionModel = params.diffusionModel,
            prompt = params.prompt,
            negativePrompt = params.negativePrompt,
            steps = params.steps,
            cfgScale = params.cfgScale,
            seed = params.seed,
            width = params.width,
            height = params.height,
            samplerName = params.samplerName,
            scheduler = params.scheduler,
            loraSelections = params.loraSelections.toList(),
        )
    }
}
