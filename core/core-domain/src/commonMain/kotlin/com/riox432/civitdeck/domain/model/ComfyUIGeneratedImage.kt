package com.riox432.civitdeck.domain.model

/**
 * Domain model representing a single generated image from ComfyUI history,
 * including the image URL and generation metadata.
 */
data class ComfyUIGeneratedImage(
    /** Unique identifier: "{promptId}/{filename}" */
    val id: String,
    val promptId: String,
    val filename: String,
    val subfolder: String,
    val type: String,
    /** Full URL constructed as: {baseUrl}/view?filename={filename}&subfolder={subfolder}&type={type} */
    val imageUrl: String,
    val meta: ComfyUIGenerationMeta,
)

/**
 * Generation metadata extracted from a ComfyUI history prompt entry.
 */
data class ComfyUIGenerationMeta(
    val positivePrompt: String = "",
    val seed: Long? = null,
    val samplerName: String? = null,
    val cfg: Double? = null,
    val steps: Int? = null,
    val loraNames: List<String> = emptyList(),
)
