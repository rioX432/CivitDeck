package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.SimilarModelHit
import com.riox432.civitdeck.domain.repository.ModelEmbeddingRepository

/**
 * Finds models whose cached image embedding is closest to the supplied query vector
 * under cosine similarity. Distinct from [GetSimilarModelsUseCase], which uses tag
 * heuristics over the CivitAI search API.
 *
 * The query vector MUST be L2-normalized. The repository performs no extra checks.
 *
 * If [sourceModelId] is provided, that model is excluded from the result so callers
 * can pass the active model's own embedding without seeing it as the top hit.
 */
class FindSimilarModelsByEmbeddingUseCase(
    private val repository: ModelEmbeddingRepository,
) {
    suspend operator fun invoke(
        query: FloatArray,
        embeddingModel: String,
        limit: Int = DEFAULT_LIMIT,
        sourceModelId: Long? = null,
    ): List<SimilarModelHit> = repository.findSimilar(
        query = query,
        embeddingModel = embeddingModel,
        limit = limit,
        excludeModelId = sourceModelId,
    )

    private companion object {
        private const val DEFAULT_LIMIT = 20
    }
}
