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
 * The newest ComfyUI history entries, flattened into [images].
 * [hasMore] is true when the server returned as many history entries as were requested:
 * `/history` reports no total, so a full window is the only signal that older entries may exist.
 * It counts history entries, not [images] — an entry can hold zero or several images.
 */
data class ComfyUIHistoryPage(
    val images: List<ComfyUIGeneratedImage>,
    val hasMore: Boolean,
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
