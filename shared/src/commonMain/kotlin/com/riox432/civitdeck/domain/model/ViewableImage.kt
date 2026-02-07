package com.riox432.civitdeck.domain.model

/**
 * Lightweight image data for full-screen viewing.
 * Bridges both [Image] (gallery) and [ModelImage] (detail carousel).
 */
data class ViewableImage(
    val url: String,
    val meta: ImageGenerationMeta?,
)

fun Image.toViewable() = ViewableImage(url = url, meta = meta)

fun ModelImage.toViewable() = ViewableImage(url = url, meta = meta)
