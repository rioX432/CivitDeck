package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached image embedding for a CivitAI model.
 *
 * The embedding is stored as a raw byte BLOB. The on-disk layout is platform-agnostic:
 * little-endian fp16 values, [dim] of them, totalling `dim * 2` bytes. fp16 is chosen
 * to halve the storage cost (~1.5 KB per 768-d vector) without measurable retrieval
 * quality loss for cosine similarity.
 *
 * One row per (modelId, modelName) — re-embedding the same model overwrites the row.
 * The [modelName] column is the embedding model identifier (e.g. "siglip2-base-p16-224"),
 * not the CivitAI model name. Future model upgrades bump the identifier so we can
 * invalidate stale rows.
 */
@Entity(tableName = "model_embeddings")
data class ModelEmbeddingEntity(
    @PrimaryKey val modelId: Long,
    val embeddingModel: String,
    val dim: Int,
    val embedding: ByteArray,
    val cachedAt: Long,
) {
    @Suppress("CyclomaticComplexMethod")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModelEmbeddingEntity) return false
        return modelId == other.modelId &&
            embeddingModel == other.embeddingModel &&
            dim == other.dim &&
            cachedAt == other.cachedAt &&
            embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int {
        var result = modelId.hashCode()
        result = HASH_PRIME * result + embeddingModel.hashCode()
        result = HASH_PRIME * result + dim
        result = HASH_PRIME * result + cachedAt.hashCode()
        result = HASH_PRIME * result + embedding.contentHashCode()
        return result
    }

    private companion object {
        private const val HASH_PRIME = 31
    }
}
