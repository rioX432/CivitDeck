package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.dao.ModelEmbeddingDao
import com.riox432.civitdeck.data.local.entity.ModelEmbeddingEntity
import com.riox432.civitdeck.domain.model.ModelEmbedding
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [ModelEmbeddingRepositoryImpl] covering fp16 round-trip caching,
 * cosine similarity ranking, exclude/dim filtering, limits, and stale cleanup.
 */
class ModelEmbeddingRepositoryImplTest {

    private class FakeDao : ModelEmbeddingDao {
        val rows = mutableListOf<ModelEmbeddingEntity>()

        override suspend fun get(modelId: Long): ModelEmbeddingEntity? =
            rows.firstOrNull { it.modelId == modelId }

        override suspend fun getAllForModel(embeddingModel: String): List<ModelEmbeddingEntity> =
            rows.filter { it.embeddingModel == embeddingModel }

        override suspend fun countFor(embeddingModel: String): Int =
            rows.count { it.embeddingModel == embeddingModel }

        override suspend fun upsert(entity: ModelEmbeddingEntity) {
            rows.removeAll { it.modelId == entity.modelId }
            rows.add(entity)
        }

        override suspend fun delete(modelId: Long): Int {
            val before = rows.size
            rows.removeAll { it.modelId == modelId }
            return before - rows.size
        }

        override suspend fun deleteStale(keepModel: String): Int {
            val before = rows.size
            rows.removeAll { it.embeddingModel != keepModel }
            return before - rows.size
        }

        override suspend fun deleteAll(): Int {
            val count = rows.size
            rows.clear()
            return count
        }
    }

    private val model = "siglip2"

    private fun embedding(modelId: Long, vector: FloatArray) = ModelEmbedding(
        modelId = modelId,
        embeddingModel = model,
        vector = vector,
        cachedAt = 1000L,
    )

    @Test
    fun cache_and_get_round_trips_vector() = runTest {
        val repo = ModelEmbeddingRepositoryImpl(FakeDao())
        repo.cache(embedding(1L, floatArrayOf(0.5f, -0.5f, 0.25f)))
        val result = repo.get(1L)
        assertEquals(1L, result?.modelId)
        // fp16 has limited precision; allow small tolerance.
        assertTrue(result!!.vector.zip(listOf(0.5f, -0.5f, 0.25f)).all { (a, b) -> kotlin.math.abs(a - b) < 0.001f })
    }

    @Test
    fun count_returns_rows_for_embedding_model() = runTest {
        val repo = ModelEmbeddingRepositoryImpl(FakeDao())
        repo.cache(embedding(1L, floatArrayOf(1f, 0f)))
        repo.cache(embedding(2L, floatArrayOf(0f, 1f)))
        assertEquals(2, repo.count(model))
        assertEquals(0, repo.count("other"))
    }

    @Test
    fun findSimilar_ranks_by_cosine_descending() = runTest {
        val repo = ModelEmbeddingRepositoryImpl(FakeDao())
        repo.cache(embedding(1L, floatArrayOf(1f, 0f)))
        repo.cache(embedding(2L, floatArrayOf(0f, 1f)))
        val hits = repo.findSimilar(floatArrayOf(1f, 0f), model, limit = 2, excludeModelId = null)
        assertEquals(1L, hits[0].modelId)
        assertTrue(hits[0].score > hits[1].score)
    }

    @Test
    fun findSimilar_excludes_given_model_id() = runTest {
        val repo = ModelEmbeddingRepositoryImpl(FakeDao())
        repo.cache(embedding(1L, floatArrayOf(1f, 0f)))
        repo.cache(embedding(2L, floatArrayOf(0f, 1f)))
        val hits = repo.findSimilar(floatArrayOf(1f, 0f), model, limit = 5, excludeModelId = 1L)
        assertEquals(listOf(2L), hits.map { it.modelId })
    }

    @Test
    fun findSimilar_skips_mismatched_dim() = runTest {
        val repo = ModelEmbeddingRepositoryImpl(FakeDao())
        repo.cache(embedding(1L, floatArrayOf(1f, 0f, 0f)))
        val hits = repo.findSimilar(floatArrayOf(1f, 0f), model, limit = 5, excludeModelId = null)
        assertTrue(hits.isEmpty())
    }

    @Test
    fun findSimilar_returns_empty_for_non_positive_limit() = runTest {
        val repo = ModelEmbeddingRepositoryImpl(FakeDao())
        repo.cache(embedding(1L, floatArrayOf(1f, 0f)))
        assertTrue(repo.findSimilar(floatArrayOf(1f, 0f), model, limit = 0, excludeModelId = null).isEmpty())
    }

    @Test
    fun deleteStale_removes_other_models() = runTest {
        val dao = FakeDao()
        dao.rows.add(ModelEmbeddingEntity(1L, model, 2, byteArrayOf(0, 0, 0, 0), 0L))
        dao.rows.add(ModelEmbeddingEntity(2L, "old", 2, byteArrayOf(0, 0, 0, 0), 0L))
        val repo = ModelEmbeddingRepositoryImpl(dao)
        assertEquals(1, repo.deleteStale(model))
        assertNull(repo.get(2L))
    }

    @Test
    fun clear_removes_all() = runTest {
        val repo = ModelEmbeddingRepositoryImpl(FakeDao())
        repo.cache(embedding(1L, floatArrayOf(1f, 0f)))
        repo.clear()
        assertNull(repo.get(1L))
    }
}
