package com.riox432.civitdeck.domain.model

import com.riox432.civitdeck.data.local.entity.SavedPromptEntity

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

fun SavedPromptEntity.toDomain(): SavedPrompt = SavedPrompt(
    id = id,
    prompt = prompt,
    negativePrompt = negativePrompt,
    sampler = sampler,
    steps = steps,
    cfgScale = cfgScale,
    seed = seed,
    modelName = modelName,
    size = size,
    sourceImageUrl = sourceImageUrl,
    savedAt = savedAt,
    isTemplate = isTemplate,
    templateName = templateName,
    autoSaved = autoSaved,
)
