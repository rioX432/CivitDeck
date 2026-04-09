package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.ml.TextEmbeddingModel
import com.riox432.civitdeck.domain.model.SimilarModelHit
import com.riox432.civitdeck.domain.repository.ModelEmbeddingRepository

/**
 * Searches cached image embeddings using a natural language text query.
 *
 * Flow:
 *  1. Check [TextEmbeddingModel.isAvailable] — if false, return empty list immediately
 *     (the UI layer checks availability separately to show "coming soon").
 *  2. Produce a text embedding via [TextEmbeddingModel.embed].
 *  3. Run cosine-similarity search against cached image embeddings via
 *     [ModelEmbeddingRepository.findSimilar].
 *
 * This works because SigLIP-2 text and image embeddings share the same 768-d vector
 * space — cosine similarity across modalities is meaningful by design.
 */
class TextSearchUseCase(
    private val textEmbeddingModel: TextEmbeddingModel,
    private val embeddingRepository: ModelEmbeddingRepository,
) {
    /** Whether the text encoder is ready for inference. */
    val isAvailable: Boolean
        get() = textEmbeddingModel.isAvailable

    /**
     * Searches for models matching the text description.
     *
     * @return ranked list of hits, or empty if the text encoder is unavailable.
     */
    suspend operator fun invoke(
        query: String,
        limit: Int = DEFAULT_LIMIT,
    ): List<SimilarModelHit> {
        if (!textEmbeddingModel.isAvailable) return emptyList()

        val textVector = textEmbeddingModel.embed(query)
        return embeddingRepository.findSimilar(
            query = textVector,
            embeddingModel = textEmbeddingModel.embeddingModelId,
            limit = limit,
        )
    }

    private companion object {
        private const val DEFAULT_LIMIT = 20
    }
}
