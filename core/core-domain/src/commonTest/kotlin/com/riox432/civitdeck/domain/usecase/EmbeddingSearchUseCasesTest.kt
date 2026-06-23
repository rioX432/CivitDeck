package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.ml.ImageEmbeddingModel
import com.riox432.civitdeck.domain.ml.TextEmbeddingModel
import com.riox432.civitdeck.domain.model.ModelEmbedding
import com.riox432.civitdeck.domain.model.SimilarModelHit
import com.riox432.civitdeck.domain.repository.ModelEmbeddingRepository
import com.riox432.civitdeck.domain.repository.ThumbnailDownloader
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the short-circuit / branching logic of the embedding-driven search use cases:
 * [TextSearchUseCase] and [EmbedOnBrowseUseCase].
 */
class EmbeddingSearchUseCasesTest {

    // --- TextSearchUseCase ---

    @Test
    fun text_search_returns_empty_when_encoder_unavailable() = runTest {
        val text = FakeTextEmbeddingModel(available = false)
        val repo = RecordingEmbeddingRepo()
        val useCase = TextSearchUseCase(text, repo)

        val result = useCase("a cat", limit = 5)

        assertTrue(result.isEmpty())
        // Must short-circuit before touching the repository.
        assertNull(repo.lastFindQuery)
    }

    @Test
    fun text_search_embeds_query_and_delegates_to_repository() = runTest {
        val vector = floatArrayOf(0.1f, 0.2f, 0.3f)
        val text = FakeTextEmbeddingModel(available = true, vector = vector)
        val repo = RecordingEmbeddingRepo(
            hits = listOf(SimilarModelHit(modelId = 42L, score = 0.9f)),
        )
        val useCase = TextSearchUseCase(text, repo)

        val result = useCase("a cat", limit = 7)

        assertEquals(listOf(42L), result.map { it.modelId })
        assertEquals(vector.toList(), repo.lastFindQuery?.toList())
        assertEquals("test-encoder", repo.lastFindModel)
        assertEquals(7, repo.lastFindLimit)
    }

    @Test
    fun text_search_exposes_encoder_availability() {
        assertTrue(TextSearchUseCase(FakeTextEmbeddingModel(available = true), RecordingEmbeddingRepo()).isAvailable)
        assertTrue(!TextSearchUseCase(FakeTextEmbeddingModel(available = false), RecordingEmbeddingRepo()).isAvailable)
    }

    // --- EmbedOnBrowseUseCase ---

    @Test
    fun embed_on_browse_no_ops_when_embedder_unavailable() = runTest {
        val repo = RecordingEmbeddingRepo()
        val downloader = RecordingDownloader()
        val useCase = EmbedOnBrowseUseCase(
            embeddingRepository = repo,
            embedImage = EmbedImageUseCase(FakeImageEmbeddingModel(available = false)),
            downloader = downloader,
        )

        useCase(modelId = 1L, thumbnailUrl = "url")

        assertNull(downloader.lastUrl)
        assertTrue(repo.cached.isEmpty())
    }

    @Test
    fun embed_on_browse_skips_when_already_cached() = runTest {
        val repo = RecordingEmbeddingRepo().apply {
            preset[1L] = ModelEmbedding(1L, "m", floatArrayOf(1f), cachedAt = 0L)
        }
        val downloader = RecordingDownloader()
        val useCase = EmbedOnBrowseUseCase(
            embeddingRepository = repo,
            embedImage = EmbedImageUseCase(FakeImageEmbeddingModel(available = true)),
            downloader = downloader,
        )

        useCase(modelId = 1L, thumbnailUrl = "url")

        // Cached already -> no download, no new cache write.
        assertNull(downloader.lastUrl)
        assertTrue(repo.cached.isEmpty())
    }

    @Test
    fun embed_on_browse_downloads_embeds_and_caches() = runTest {
        val embedded = floatArrayOf(0.5f, 0.5f)
        val repo = RecordingEmbeddingRepo()
        val downloader = RecordingDownloader(bytes = byteArrayOf(1, 2, 3))
        val useCase = EmbedOnBrowseUseCase(
            embeddingRepository = repo,
            embedImage = EmbedImageUseCase(FakeImageEmbeddingModel(available = true, vector = embedded)),
            downloader = downloader,
        )

        useCase(modelId = 99L, thumbnailUrl = "https://thumb")

        assertEquals("https://thumb", downloader.lastUrl)
        val cached = repo.cached.single()
        assertEquals(99L, cached.modelId)
        assertEquals(embedded.toList(), cached.vector.toList())
    }

    // --- Fakes ---

    private class FakeTextEmbeddingModel(
        private val available: Boolean,
        private val vector: FloatArray = floatArrayOf(),
    ) : TextEmbeddingModel {
        override val isAvailable: Boolean = available
        override val embeddingModelId: String = "test-encoder"
        override suspend fun embed(text: String): FloatArray = vector
    }

    private class FakeImageEmbeddingModel(
        private val available: Boolean,
        private val vector: FloatArray = floatArrayOf(),
    ) : ImageEmbeddingModel {
        override val isAvailable: Boolean = available
        override suspend fun embed(imageBytes: ByteArray): FloatArray = vector
    }

    private class RecordingDownloader(private val bytes: ByteArray = ByteArray(0)) :
        ThumbnailDownloader {
        var lastUrl: String? = null
        override suspend fun download(url: String): ByteArray {
            lastUrl = url
            return bytes
        }
    }

    private class RecordingEmbeddingRepo(
        private val hits: List<SimilarModelHit> = emptyList(),
    ) : ModelEmbeddingRepository {
        val preset = mutableMapOf<Long, ModelEmbedding>()
        val cached = mutableListOf<ModelEmbedding>()
        var lastFindQuery: FloatArray? = null
        var lastFindModel: String? = null
        var lastFindLimit: Int? = null

        override suspend fun get(modelId: Long): ModelEmbedding? = preset[modelId]
        override suspend fun count(embeddingModel: String): Int = 0
        override suspend fun cache(embedding: ModelEmbedding) {
            cached.add(embedding)
        }
        override suspend fun findSimilar(
            query: FloatArray,
            embeddingModel: String,
            limit: Int,
            excludeModelId: Long?,
        ): List<SimilarModelHit> {
            lastFindQuery = query
            lastFindModel = embeddingModel
            lastFindLimit = limit
            return hits
        }
        override suspend fun deleteStale(keepModel: String): Int = 0
        override suspend fun clear() = Unit
    }
}
