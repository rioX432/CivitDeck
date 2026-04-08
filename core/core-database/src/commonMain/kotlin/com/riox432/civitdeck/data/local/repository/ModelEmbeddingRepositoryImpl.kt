package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.dao.ModelEmbeddingDao
import com.riox432.civitdeck.data.local.entity.ModelEmbeddingEntity
import com.riox432.civitdeck.domain.model.ModelEmbedding
import com.riox432.civitdeck.domain.model.SimilarModelHit
import com.riox432.civitdeck.domain.repository.ModelEmbeddingRepository
import com.riox432.civitdeck.domain.util.currentTimeMillis

class ModelEmbeddingRepositoryImpl(
    private val dao: ModelEmbeddingDao,
) : ModelEmbeddingRepository {

    override suspend fun get(modelId: Long): ModelEmbedding? =
        dao.get(modelId)?.toDomain()

    override suspend fun count(embeddingModel: String): Int =
        dao.countFor(embeddingModel)

    override suspend fun cache(embedding: ModelEmbedding) {
        dao.upsert(
            ModelEmbeddingEntity(
                modelId = embedding.modelId,
                embeddingModel = embedding.embeddingModel,
                dim = embedding.dim,
                embedding = Fp16.encode(embedding.vector),
                cachedAt = if (embedding.cachedAt > 0) embedding.cachedAt else currentTimeMillis(),
            ),
        )
    }

    override suspend fun findSimilar(
        query: FloatArray,
        embeddingModel: String,
        limit: Int,
        excludeModelId: Long?,
    ): List<SimilarModelHit> {
        if (limit <= 0) return emptyList()
        val rows = dao.getAllForModel(embeddingModel)
        if (rows.isEmpty()) return emptyList()

        val scored = ArrayList<SimilarModelHit>(rows.size)
        for (row in rows) {
            if (excludeModelId != null && row.modelId == excludeModelId) continue
            if (row.dim != query.size) continue
            val vector = Fp16.decode(row.embedding, row.dim)
            scored += SimilarModelHit(modelId = row.modelId, score = cosine(query, vector))
        }
        scored.sortByDescending { it.score }
        return if (scored.size > limit) scored.subList(0, limit).toList() else scored
    }

    override suspend fun deleteStale(keepModel: String): Int = dao.deleteStale(keepModel)

    override suspend fun clear() {
        dao.deleteAll()
    }

    private fun ModelEmbeddingEntity.toDomain() = ModelEmbedding(
        modelId = modelId,
        embeddingModel = embeddingModel,
        vector = Fp16.decode(embedding, dim),
        cachedAt = cachedAt,
    )

    /**
     * Vectors arriving here MUST already be L2-normalized, so cosine reduces to a dot product.
     * No defensive renormalization — silent normalization would mask caller bugs.
     */
    private fun cosine(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) sum += a[i] * b[i]
        return sum
    }
}
