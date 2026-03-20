package com.riox432.civitdeck.domain.model

/**
 * A published workflow from ComfyHub (via CivitAI's workflow-sharing endpoints).
 */
data class ComfyHubWorkflow(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val tags: List<String>,
    val category: String,
    val previewImageUrl: String?,
    val nodeCount: Int,
    val downloads: Int,
    val rating: Double,
    val workflowJson: String,
)

enum class ComfyHubCategory(val displayName: String) {
    ALL("All"),
    TXT2IMG("Text to Image"),
    IMG2IMG("Image to Image"),
    INPAINTING("Inpainting"),
    UPSCALE("Upscale"),
    CONTROLNET("ControlNet"),
    VIDEO("Video"),
    ANIMATION("Animation"),
    OTHER("Other"),
}

enum class ComfyHubSortOrder(val displayName: String) {
    MOST_DOWNLOADED("Most Downloaded"),
    HIGHEST_RATED("Highest Rated"),
    NEWEST("Newest"),
}
