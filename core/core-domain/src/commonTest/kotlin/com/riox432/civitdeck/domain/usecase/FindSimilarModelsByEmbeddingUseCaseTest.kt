package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ModelEmbedding
import com.riox432.civitdeck.domain.model.SimilarModelHit
import com.riox432.civitdeck.domain.repository.ModelEmbeddingRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FindSimilarModelsByEmbeddingUseCaseTest {

    private val embeddingModel = "siglip2-base-p16-224"

    @Test
    fun returns_top_n_sorted_descending_by_cosine() = runTest {
        val repo = FakeModelEmbeddingRepository().apply {
            cache(embedding(modelId = 1L, vector = floatArrayOf(1f, 0f, 0f)))
            cache(embedding(modelId = 2L, vector = normalize(floatArrayOf(1f, 1f, 0f))))
            cache(embedding(modelId = 3L, vector = floatArrayOf(0f, 1f, 0f)))
            cache(embedding(modelId = 4L, vector = floatArrayOf(0f, 0f, 1f)))
        }
        val useCase = FindSimilarModelsByEmbeddingUseCase(repo)

        val result = useCase(
            query = floatArrayOf(1f, 0f, 0f),
            embeddingModel = embeddingModel,
            limit = 3,
        )

        assertEquals(listOf(1L, 2L, 3L), result.map { it.modelId })
        // model 4 (orthogonal to query) is dropped by the limit
        assertTrue(result[0].score > result[1].score)
        assertTrue(result[1].score > result[2].score)
    }

    @Test
    fun excludes_source_model_when_requested() = runTest {
        val repo = FakeModelEmbeddingRepository().apply {
            cache(embedding(modelId = 1L, vector = floatArrayOf(1f, 0f)))
            cache(embedding(modelId = 2L, vector = floatArrayOf(0f, 1f)))
        }
        val useCase = FindSimilarModelsByEmbeddingUseCase(repo)

        val result = useCase(
            query = floatArrayOf(1f, 0f),
            embeddingModel = embeddingModel,
            limit = 5,
            sourceModelId = 1L,
        )

        assertEquals(listOf(2L), result.map { it.modelId })
    }

    @Test
    fun returns_empty_when_no_cached_embeddings_match_model() = runTest {
        val repo = FakeModelEmbeddingRepository()
        val useCase = FindSimilarModelsByEmbeddingUseCase(repo)

        val result = useCase(
            query = floatArrayOf(1f, 0f),
            embeddingModel = embeddingModel,
            limit = 5,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun ignores_rows_whose_dimension_does_not_match_query() = runTest {
        val repo = FakeModelEmbeddingRepository().apply {
            cache(embedding(modelId = 1L, vector = floatArrayOf(1f, 0f, 0f, 0f)))
            cache(embedding(modelId = 2L, vector = floatArrayOf(1f, 0f)))
        }
        val useCase = FindSimilarModelsByEmbeddingUseCase(repo)

        val result = useCase(
            query = floatArrayOf(1f, 0f),
            embeddingModel = embeddingModel,
            limit = 5,
        )

        assertEquals(listOf(2L), result.map { it.modelId })
    }

    private fun embedding(modelId: Long, vector: FloatArray) = ModelEmbedding(
        modelId = modelId,
        embeddingModel = embeddingModel,
        vector = vector,
        cachedAt = 0L,
    )

    private fun normalize(v: FloatArray): FloatArray {
        var sum = 0f
        for (x in v) sum += x * x
        val norm = kotlin.math.sqrt(sum)
        return FloatArray(v.size) { v[it] / norm }
    }
}

private class FakeModelEmbeddingRepository : ModelEmbeddingRepository {
    private val store = mutableMapOf<Long, ModelEmbedding>()

    override suspend fun get(modelId: Long): ModelEmbedding? = store[modelId]

    override suspend fun count(embeddingModel: String): Int =
        store.values.count { it.embeddingModel == embeddingModel }

    override suspend fun cache(embedding: ModelEmbedding) {
        store[embedding.modelId] = embedding
    }

    override suspend fun findSimilar(
        query: FloatArray,
        embeddingModel: String,
        limit: Int,
        excludeModelId: Long?,
    ): List<SimilarModelHit> {
        val hits = store.values
            .filter { it.embeddingModel == embeddingModel }
            .filter { excludeModelId == null || it.modelId != excludeModelId }
            .filter { it.vector.size == query.size }
            .map { SimilarModelHit(modelId = it.modelId, score = dot(query, it.vector)) }
            .sortedByDescending { it.score }
        return if (hits.size > limit) hits.take(limit) else hits
    }

    override suspend fun deleteStale(keepModel: String): Int {
        val before = store.size
        store.entries.removeAll { it.value.embeddingModel != keepModel }
        return before - store.size
    }

    override suspend fun clear() {
        store.clear()
    }

    private fun dot(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) sum += a[i] * b[i]
        return sum
    }
}
