package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.dao.ModelEmbeddingDao
import com.riox432.civitdeck.data.local.entity.ModelEmbeddingEntity
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Golden-query top-K test for semantic ranking (issue #990).
 *
 * Semantic quality is not an E2E concern — it is a ranking property that must be pinned
 * deterministically. This fixes a small labelled corpus of unit vectors over four concept
 * axes and a set of golden queries with expected top-K results, then asserts the cosine
 * ranking in [ModelEmbeddingRepositoryImpl.findSimilar] reproduces them. The corpus is
 * stored through the real Fp16 codec so half-precision loss is exercised, not bypassed.
 *
 * Concept axes (index): 0 = anime, 1 = realistic, 2 = landscape, 3 = portrait.
 */
class SemanticRankingGoldenTest {

    private val model = "siglip2-base-p16-224"

    // Labelled corpus: each model is an L2-normalized blend of concept axes.
    private val corpus = mapOf(
        PURE_ANIME to normalize(floatArrayOf(1f, 0f, 0f, 0f)),
        ANIME_PORTRAIT to normalize(floatArrayOf(0.9f, 0f, 0f, 0.45f)),
        ANIME_LANDSCAPE to normalize(floatArrayOf(0.9f, 0f, 0.45f, 0f)),
        REALISTIC_PORTRAIT to normalize(floatArrayOf(0f, 0.9f, 0f, 0.45f)),
        REALISTIC_LANDSCAPE to normalize(floatArrayOf(0f, 0.9f, 0.45f, 0f)),
    )

    private fun repository(): ModelEmbeddingRepositoryImpl {
        val rows = corpus.map { (id, vector) ->
            ModelEmbeddingEntity(
                modelId = id,
                embeddingModel = model,
                dim = vector.size,
                embedding = Fp16.encode(vector),
                cachedAt = 1L,
            )
        }
        return ModelEmbeddingRepositoryImpl(FakeModelEmbeddingDao(rows))
    }

    @Test
    fun animeQueryRanksPureAnimeFirst() = runTest {
        val hits = repository().findSimilar(normalize(floatArrayOf(1f, 0f, 0f, 0f)), model, limit = 3)
        assertEquals(PURE_ANIME, hits.first().modelId, "pure anime must be the closest hit")
        val top3 = hits.map { it.modelId }.toSet()
        // The two anime blends outrank every realistic model.
        assertTrue(ANIME_PORTRAIT in top3 && ANIME_LANDSCAPE in top3, "top-3 must be the anime cluster")
        assertTrue(REALISTIC_PORTRAIT !in top3 && REALISTIC_LANDSCAPE !in top3)
    }

    @Test
    fun realisticPortraitQueryRanksRealisticPortraitFirst() = runTest {
        val hits = repository().findSimilar(normalize(floatArrayOf(0f, 0.9f, 0f, 0.45f)), model, limit = 5)
        assertEquals(REALISTIC_PORTRAIT, hits.first().modelId)
        // Cross-modal: a realistic-portrait query must never surface an anime model above
        // the realistic ones.
        val ids = hits.map { it.modelId }
        assertTrue(ids.indexOf(REALISTIC_LANDSCAPE) < ids.indexOf(ANIME_PORTRAIT))
    }

    @Test
    fun scoresAreMonotonicallyDescending() = runTest {
        val hits = repository().findSimilar(normalize(floatArrayOf(1f, 0f, 0f, 0f)), model, limit = 5)
        val scores = hits.map { it.score }
        assertEquals(scores.sortedDescending(), scores, "hits must be ordered by descending score")
    }

    @Test
    fun limitCapsResultCount() = runTest {
        assertEquals(2, repository().findSimilar(normalize(floatArrayOf(1f, 0f, 0f, 0f)), model, limit = 2).size)
    }

    private fun normalize(v: FloatArray): FloatArray {
        var norm = 0f
        for (x in v) norm += x * x
        val inv = 1f / sqrt(norm)
        return FloatArray(v.size) { v[it] * inv }
    }

    private companion object {
        const val PURE_ANIME = 1L
        const val ANIME_PORTRAIT = 2L
        const val ANIME_LANDSCAPE = 3L
        const val REALISTIC_PORTRAIT = 4L
        const val REALISTIC_LANDSCAPE = 5L
    }
}

private class FakeModelEmbeddingDao(
    private val rows: List<ModelEmbeddingEntity>,
) : ModelEmbeddingDao {
    override suspend fun get(modelId: Long): ModelEmbeddingEntity? = rows.firstOrNull { it.modelId == modelId }
    override suspend fun getAllForModel(embeddingModel: String): List<ModelEmbeddingEntity> =
        rows.filter { it.embeddingModel == embeddingModel }
    override suspend fun countFor(embeddingModel: String): Int =
        rows.count { it.embeddingModel == embeddingModel }
    override suspend fun upsert(entity: ModelEmbeddingEntity) = error("not used")
    override suspend fun delete(modelId: Long): Int = error("not used")
    override suspend fun deleteStale(keepModel: String): Int = error("not used")
    override suspend fun deleteAll(): Int = error("not used")
}
