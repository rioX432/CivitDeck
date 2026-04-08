package com.riox432.civitdeck.domain.model

/**
 * Domain representation of a cached image embedding for a CivitAI model.
 *
 * The vector is L2-normalized so cosine similarity reduces to a dot product.
 * Stored in fp16 on disk and decoded to fp32 [FloatArray] for in-memory math.
 */
data class ModelEmbedding(
    val modelId: Long,
    val embeddingModel: String,
    val vector: FloatArray,
    val cachedAt: Long,
) {
    val dim: Int get() = vector.size

    @Suppress("CyclomaticComplexMethod")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModelEmbedding) return false
        return modelId == other.modelId &&
            embeddingModel == other.embeddingModel &&
            cachedAt == other.cachedAt &&
            vector.contentEquals(other.vector)
    }

    override fun hashCode(): Int {
        var result = modelId.hashCode()
        result = HASH_PRIME * result + embeddingModel.hashCode()
        result = HASH_PRIME * result + cachedAt.hashCode()
        result = HASH_PRIME * result + vector.contentHashCode()
        return result
    }

    private companion object {
        private const val HASH_PRIME = 31
    }
}

/**
 * A neighbour returned from a similarity search, paired with its cosine score in [-1, 1].
 */
data class SimilarModelHit(
    val modelId: Long,
    val score: Float,
)
