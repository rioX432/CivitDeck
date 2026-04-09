package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ModelEmbedding
import com.riox432.civitdeck.domain.repository.ModelEmbeddingRepository
import com.riox432.civitdeck.domain.repository.ThumbnailDownloader

/**
 * Automatically embeds a model's thumbnail image when the user browses or favorites it.
 *
 * The embedding is produced by [EmbedImageUseCase] (which delegates to the platform
 * [ImageEmbeddingModel][com.riox432.civitdeck.domain.ml.ImageEmbeddingModel]) and cached
 * via [ModelEmbeddingRepository] so that "Find Similar" can use it later.
 *
 * Short-circuits silently when:
 * - an embedding is already cached for this model
 * - the on-device embedder is unavailable (e.g. Desktop)
 *
 * Callers should wrap invocations in `runCatching` — this use case must never block UI.
 */
class EmbedOnBrowseUseCase(
    private val embeddingRepository: ModelEmbeddingRepository,
    private val embedImage: EmbedImageUseCase,
    private val downloader: ThumbnailDownloader,
) {
    /**
     * Downloads the thumbnail at [thumbnailUrl], embeds it, and caches the result
     * for [modelId]. No-op if the embedding is already cached or the embedder is
     * unavailable on this platform.
     */
    suspend operator fun invoke(modelId: Long, thumbnailUrl: String) {
        if (!embedImage.isAvailable) return
        if (embeddingRepository.get(modelId) != null) return

        val imageBytes = downloader.download(thumbnailUrl)
        val vector = embedImage(imageBytes)
        embeddingRepository.cache(
            ModelEmbedding(
                modelId = modelId,
                embeddingModel = EMBEDDING_MODEL_ID,
                vector = vector,
                cachedAt = 0L, // repository fills in current time when cachedAt <= 0
            ),
        )
    }

    private companion object {
        /** Matches the SigLIP-2 base model used across all platforms. */
        private const val EMBEDDING_MODEL_ID = "siglip2-base-p16-224"
    }
}
