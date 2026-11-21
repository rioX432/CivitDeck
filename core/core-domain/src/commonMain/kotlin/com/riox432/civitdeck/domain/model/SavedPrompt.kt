package com.riox432.civitdeck.domain.model

data class SavedPrompt(
    val id: Long,
    val prompt: String,
    val negativePrompt: String?,
    val sampler: String?,
    val steps: Int?,
    val cfgScale: Double?,
    val seed: Long?,
    val modelName: String?,
    val size: String?,
    val sourceImageUrl: String?,
    val savedAt: Long,
    val isTemplate: Boolean = false,
    val templateName: String? = null,
    val autoSaved: Boolean = false,
    val templateVariables: String? = null,
    val templateType: String? = null,
    val templateMetadata: String? = null,
    val isAppMode: Boolean = false,
    val rawWorkflowJson: String? = null,
    /**
     * The source snapshot captured when this prompt was saved from an artwork, or null when none
     * was. Malformed or newer-version snapshots are reported as such instead of being dropped.
     * Saving a template never writes this field.
     */
    val sourceReference: SourceReferenceDecodeResult? = null,
)
