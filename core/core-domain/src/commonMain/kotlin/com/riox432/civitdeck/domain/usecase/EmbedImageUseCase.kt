package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.ml.ImageEmbeddingModel

/**
 * Runs the platform image embedder over raw image bytes and returns a normalized vector.
 *
 * The use case is the only place that talks to [ImageEmbeddingModel] directly — callers
 * should depend on it instead so the platform stub vs. real implementation is invisible.
 */
class EmbedImageUseCase(
    private val embedder: ImageEmbeddingModel,
) {
    val isAvailable: Boolean get() = embedder.isAvailable

    suspend operator fun invoke(imageBytes: ByteArray): FloatArray = embedder.embed(imageBytes)
}
