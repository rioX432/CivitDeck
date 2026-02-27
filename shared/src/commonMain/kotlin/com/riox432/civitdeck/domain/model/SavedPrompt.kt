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
)
