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
    /**
     * The server this output was read from, captured before the request. Null for outputs built
     * without one, which must stay unlinked: [imageUrl] alone does not identify a saved connection.
     */
    val jobTarget: ComfyUIJobTarget? = null,
)

/**
 * The remote outcome ComfyUI recorded for a history entry. Only an explicit status or event
 * decides it; output images alone never mean [Completed], and missing output never means
 * [Interrupted].
 */
enum class ComfyUIHistoryOutcome {
    Completed,
    Failed,
    Interrupted,

    /** Missing, still running, or a status this app cannot read. */
    Unknown,
}

/**
 * The history of one bound [job], read from [job]'s own server. [present] is false when that
 * server has no entry for the prompt. A failed entry may still carry partial [images].
 * [error] is the server's exception message for a [ComfyUIHistoryOutcome.Failed] entry, if any.
 */
data class ComfyUIBoundHistoryItem(
    val job: ComfyUIJobHandle,
    val present: Boolean,
    val outcome: ComfyUIHistoryOutcome,
    val images: List<ComfyUIGeneratedImage>,
    val error: String? = null,
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
