package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ModelEmbedding
import com.riox432.civitdeck.domain.model.SimilarModelHit

/**
 * Persistence layer for cached SigLIP-2 image embeddings.
 *
 * The repository deals in fp32 [FloatArray] vectors at the API boundary; the storage
 * representation (fp16 BLOB) is an implementation detail.
 *
 * Vectors passed in MUST be L2-normalized — callers are responsible. The repository
 * does not re-normalize, both to keep the math honest and to make malformed input
 * detectable in tests.
 */
interface ModelEmbeddingRepository {

    /** Returns the cached embedding for [modelId], or null if none exists. */
    suspend fun get(modelId: Long): ModelEmbedding?

    /** Number of cached embeddings produced by [embeddingModel]. */
    suspend fun count(embeddingModel: String): Int

    /** Insert or replace the cached embedding for a model. */
    suspend fun cache(embedding: ModelEmbedding)

    /**
     * Cosine-similarity search across cached embeddings produced by [embeddingModel].
     *
     * Implementations scan in memory — fine for the MVP scale (<10 k vectors).
     * If [excludeModelId] is provided, that model is omitted from the result.
     */
    suspend fun findSimilar(
        query: FloatArray,
        embeddingModel: String,
        limit: Int,
        excludeModelId: Long? = null,
    ): List<SimilarModelHit>

    /** Drop every cached embedding whose [embeddingModel] differs from [keepModel]. */
    suspend fun deleteStale(keepModel: String): Int

    /** Drop every cached embedding. */
    suspend fun clear()
}
