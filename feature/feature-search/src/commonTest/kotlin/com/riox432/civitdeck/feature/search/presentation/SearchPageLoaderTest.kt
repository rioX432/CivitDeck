package com.riox432.civitdeck.feature.search.presentation

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelReference
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.ModelStats
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.repository.HuggingFaceRepository
import com.riox432.civitdeck.domain.repository.TensorArtRepository
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.QualityScoreCalculator
import com.riox432.civitdeck.feature.search.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.MultiSourceSearchUseCase
import com.riox432.civitdeck.testing.FakeBrowsingHistoryRepository
import com.riox432.civitdeck.testing.FakeModelRepository
import com.riox432.civitdeck.testing.testModel
import com.riox432.civitdeck.testing.testPaginatedResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the NSFW boundary in [SearchPageLoader.loadCivitaiPage]: the `/models`
 * `nsfw` parameter must always be sent explicitly (false for Off, true otherwise) —
 * omitting it makes the API strip non-PG images, leaving NSFW models thumbnail-less.
 * Finer level filtering (Soft) happens client-side via `filterNsfwImages`.
 *
 * Also covers per-sort paging: only Most Downloaded has a client key that follows the
 * API order, so only it keeps the cross-page sort watermark.
 *
 * Multi-source tests cover per-provider continuation, cross-page dedup, and that a reset or
 * cancelled request never commits its session state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchPageLoaderTest {

    private class NoOpHuggingFaceRepo : HuggingFaceRepository {
        override suspend fun searchModels(query: String?, limit: Int, offset: Int): List<Model> =
            emptyList()
    }

    private class NoOpTensorArtRepo : TensorArtRepository {
        override suspend fun searchModels(query: String, page: Int, pageSize: Int): List<Model> =
            emptyList()
    }

    private class PagedHuggingFaceRepo(
        private val gate: CompletableDeferred<Unit>? = null,
        private val pages: (offset: Int) -> List<Model>,
    ) : HuggingFaceRepository {
        val requestedOffsets = mutableListOf<Int>()

        override suspend fun searchModels(query: String?, limit: Int, offset: Int): List<Model> {
            requestedOffsets += offset
            gate?.await()
            return pages(offset)
        }
    }

    private class FixedTensorArtRepo(private val models: List<Model>) : TensorArtRepository {
        override suspend fun searchModels(query: String, page: Int, pageSize: Int): List<Model> =
            if (page == 1) models else emptyList()
    }

    private fun loaderWith(
        modelRepo: FakeModelRepository,
        huggingFaceRepo: HuggingFaceRepository = NoOpHuggingFaceRepo(),
        tensorArtRepo: TensorArtRepository = NoOpTensorArtRepo(),
    ): SearchPageLoader {
        val browsingRepo = FakeBrowsingHistoryRepository()
        return SearchPageLoader(
            getModelsUseCase = GetModelsUseCase(modelRepo),
            multiSourceSearchUseCase = MultiSourceSearchUseCase(
                modelRepository = modelRepo,
                huggingFaceRepository = huggingFaceRepo,
                tensorArtRepository = tensorArtRepo,
            ),
            getViewedModelIdsUseCase = GetViewedModelIdsUseCase(browsingRepo),
            hiddenModelIds = MutableStateFlow(emptySet()),
        )
    }

    @Test
    fun nsfwFilterOff_sends_nsfw_false() = runTest {
        val modelRepo = FakeModelRepository(
            pages = listOf(testPaginatedResult(items = listOf(testModel(id = 1L)))),
        )
        val loader = loaderWith(modelRepo)

        loader.loadPage(
            filter = FilterState(nsfwFilterLevel = NsfwFilterLevel.Off),
            cursor = null,
            limit = 20,
        )

        assertEquals(false, modelRepo.lastQuery?.nsfw)
    }

    @Test
    fun nsfwFilterSoft_sends_nsfw_true() = runTest {
        val modelRepo = FakeModelRepository(
            pages = listOf(testPaginatedResult(items = listOf(testModel(id = 1L)))),
        )
        val loader = loaderWith(modelRepo)

        loader.loadPage(
            filter = FilterState(nsfwFilterLevel = NsfwFilterLevel.Soft),
            cursor = null,
            limit = 20,
        )

        assertEquals(true, modelRepo.lastQuery?.nsfw)
    }

    @Test
    fun nsfwFilterAll_sends_nsfw_true() = runTest {
        val modelRepo = FakeModelRepository(
            pages = listOf(testPaginatedResult(items = listOf(testModel(id = 1L)))),
        )
        val loader = loaderWith(modelRepo)

        loader.loadPage(
            filter = FilterState(nsfwFilterLevel = NsfwFilterLevel.All),
            cursor = null,
            limit = 20,
        )

        assertEquals(true, modelRepo.lastQuery?.nsfw)
    }

    @Test
    fun soft_filter_drops_images_above_soft_and_imageless_models() = runTest {
        val softModel = testModel(id = 1L)
        val explicitOnlyModel = testModel(
            id = 2L,
            modelVersions = listOf(
                com.riox432.civitdeck.testing.testModelVersion(
                    modelId = 2L,
                    nsfwLevel = com.riox432.civitdeck.domain.model.NsfwLevel.X,
                ),
            ),
        )
        val modelRepo = FakeModelRepository(
            pages = listOf(testPaginatedResult(items = listOf(softModel, explicitOnlyModel))),
        )
        val loader = loaderWith(modelRepo)

        val result = loader.loadPage(
            filter = FilterState(nsfwFilterLevel = NsfwFilterLevel.Soft),
            cursor = null,
            limit = 20,
        )

        assertEquals(listOf(1L), result.items.map { it.id })
    }

    @Test
    fun resetPagination_clears_watermark() = runTest {
        val modelRepo = FakeModelRepository(
            pages = listOf(testPaginatedResult(items = listOf(testModel(id = 1L)))),
        )
        val loader = loaderWith(modelRepo)
        loader.loadPage(FilterState(), cursor = null, limit = 20)
        loader.resetPagination()
        assertNull(loader.sortWatermark)
        assertNull(loader.multiSourceContinuation)
    }

    // FakeModelRepository serves pages by call count, not by cursor, so each load passes
    // `limit` equal to its fixture page size.

    @Test
    fun newest_keeps_api_order_and_returns_next_page_ids_above_previous_minimum() = runTest {
        val modelRepo = FakeModelRepository(
            pages = listOf(
                testPaginatedResult(items = modelsWithIds(300L, 50L, 200L), nextCursor = "c1"),
                testPaginatedResult(items = modelsWithIds(250L, 120L), nextCursor = "c2"),
            ),
        )
        val loader = loaderWith(modelRepo)

        val first = loader.loadIds(SortOrder.Newest, cursor = null, limit = 3)
        val second = loader.loadIds(SortOrder.Newest, cursor = "c1", limit = 2)

        assertEquals(listOf(300L, 50L, 200L), first)
        assertEquals(listOf(250L, 120L), second)
        assertEquals(2, modelRepo.getModelsCallCount)
    }

    @Test
    fun highestRated_returns_next_page_models_rated_above_previous_minimum() = runTest {
        val modelRepo = FakeModelRepository(
            pages = listOf(
                testPaginatedResult(
                    items = listOf(modelWith(id = 1L, rating = 4.9), modelWith(id = 2L, rating = 3.0)),
                    nextCursor = "c1",
                ),
                testPaginatedResult(
                    items = listOf(modelWith(id = 3L, rating = 4.2), modelWith(id = 4L, rating = 4.8)),
                    nextCursor = "c2",
                ),
            ),
        )
        val loader = loaderWith(modelRepo)

        loader.loadIds(SortOrder.HighestRated, cursor = null, limit = 2)
        val second = loader.loadIds(SortOrder.HighestRated, cursor = "c1", limit = 2)

        assertEquals(listOf(3L, 4L), second)
    }

    @Test
    fun mostDownloaded_drops_next_page_model_downloaded_more_than_previous_minimum() = runTest {
        val modelRepo = FakeModelRepository(
            pages = listOf(
                testPaginatedResult(
                    items = listOf(modelWith(id = 1L, downloads = 1000), modelWith(id = 2L, downloads = 500)),
                    nextCursor = "c1",
                ),
                testPaginatedResult(
                    items = listOf(modelWith(id = 3L, downloads = 600), modelWith(id = 4L, downloads = 400)),
                ),
            ),
        )
        val loader = loaderWith(modelRepo)

        loader.loadIds(SortOrder.MostDownloaded, cursor = null, limit = 2)
        val second = loader.loadIds(SortOrder.MostDownloaded, cursor = "c1", limit = 2)

        assertEquals(listOf(4L), second)
    }

    @Test
    fun quality_sorts_each_page_by_score_without_dropping_next_page_models() = runTest {
        val high = ModelStats(
            downloadCount = 1000,
            favoriteCount = 100,
            commentCount = 50,
            ratingCount = 20,
            rating = 5.0,
        )
        val low = ModelStats(downloadCount = 100, favoriteCount = 1, commentCount = 0, ratingCount = 0, rating = 0.0)
        val mid = testModel().stats
        // Page 2's model must score above page 1's minimum, or a watermark would not drop it.
        assertTrue(QualityScoreCalculator.calculate(mid) > QualityScoreCalculator.calculate(low))
        val modelRepo = FakeModelRepository(
            pages = listOf(
                testPaginatedResult(
                    items = listOf(testModel(id = 1L, stats = low), testModel(id = 2L, stats = high)),
                    nextCursor = "c1",
                ),
                testPaginatedResult(items = listOf(testModel(id = 3L, stats = mid)), nextCursor = "c2"),
            ),
        )
        val loader = loaderWith(modelRepo)

        val first = loader.loadIds(SortOrder.Quality, cursor = null, limit = 2)
        val second = loader.loadIds(SortOrder.Quality, cursor = "c1", limit = 1)

        assertEquals(listOf(2L, 1L), first)
        assertEquals(listOf(3L), second)
    }

    @Test
    fun short_last_page_without_next_cursor_is_fetched_once() = runTest {
        val modelRepo = FakeModelRepository(
            pages = listOf(testPaginatedResult(items = modelsWithIds(1L))),
        )
        val loader = loaderWith(modelRepo)

        val result = loader.loadPage(FilterState(), cursor = null, limit = SearchPageLoader.PAGE_SIZE)

        assertEquals(listOf(1L), result.items.map { it.id })
        assertNull(result.nextCursor)
        assertEquals(1, modelRepo.getModelsCallCount)
    }

    @Test
    fun next_cursor_equal_to_requested_cursor_stops_fetching() = runTest {
        val modelRepo = FakeModelRepository(
            pages = listOf(testPaginatedResult(items = modelsWithIds(1L), nextCursor = "c1")),
        )
        val loader = loaderWith(modelRepo)

        loader.loadPage(FilterState(), cursor = "c1", limit = SearchPageLoader.PAGE_SIZE)

        assertEquals(1, modelRepo.getModelsCallCount)
    }

    // --- Multi-source sessions ---

    @Test
    fun multiSource_sameNumberFromDifferentProviders_allSurvive_andRepeatedReferenceIsDropped() = runTest {
        val modelRepo = FakeModelRepository(pages = listOf(testPaginatedResult(items = listOf(testModel(id = 1L)))))
        val hfRepo = PagedHuggingFaceRepo { offset ->
            when (offset) {
                0 -> listOf(hfModel(1L, "owner/x")) + (2..PAGE).map { hfModel(it.toLong(), "owner/m$it") }
                else -> listOf(hfModel(1L, "owner/x"), hfModel(99L, "owner/new"))
            }
        }
        val taRepo = FixedTensorArtRepo(listOf(taModel(1L)))
        val loader = loaderWith(modelRepo, hfRepo, taRepo)
        val filter = FilterState(selectedSources = ALL_SOURCES)

        val first = loader.loadPage(filter, cursor = null, limit = PAGE)
        val second = loader.loadPage(filter, cursor = first.nextCursor, limit = PAGE)

        val firstKeys = first.items.map { it.reference.stableKey }
        assertTrue("CIVITAI:1" in firstKeys)
        assertTrue("HUGGING_FACE:owner/x" in firstKeys)
        assertTrue("TENSOR_ART:1" in firstKeys)
        assertEquals(listOf("HUGGING_FACE:owner/new"), second.items.map { it.reference.stableKey })
    }

    @Test
    fun multiSource_fullyFilteredRawPage_stillContinuesFromRawOffset() = runTest {
        val hfRepo = PagedHuggingFaceRepo { offset ->
            (offset until offset + PAGE).map { hfModel(it.toLong(), "owner/m$it", tags = listOf("bad")) }
        }
        val loader = loaderWith(FakeModelRepository(), hfRepo)
        val filter = FilterState(selectedSources = setOf(ModelSource.HUGGING_FACE), excludedTags = listOf("bad"))

        val first = loader.loadPage(filter, cursor = null, limit = PAGE)
        loader.loadPage(filter, cursor = first.nextCursor, limit = PAGE)

        assertTrue(first.items.isEmpty())
        assertNotNull(first.nextCursor)
        assertEquals(listOf(0, PAGE), hfRepo.requestedOffsets)
    }

    @Test
    fun multiSource_reset_startsNewSessionFromFirstOffset() = runTest {
        val hfRepo = PagedHuggingFaceRepo { offset ->
            (offset until offset + PAGE).map { hfModel(it.toLong(), "owner/m$it") }
        }
        val loader = loaderWith(FakeModelRepository(), hfRepo)
        val filter = FilterState(selectedSources = setOf(ModelSource.HUGGING_FACE))

        val first = loader.loadPage(filter, cursor = null, limit = PAGE)
        loader.loadPage(filter, cursor = first.nextCursor, limit = PAGE)
        loader.resetPagination()
        val refreshed = loader.loadPage(filter, cursor = null, limit = PAGE)

        assertEquals(listOf(0, PAGE, 0), hfRepo.requestedOffsets)
        // Seen keys were cleared with the session, so the first page shows again.
        assertEquals(first.items.map { it.id }, refreshed.items.map { it.id })
    }

    @Test
    fun multiSource_resultFinishingAfterReset_isCancelledAndNotCommitted() = runTest {
        val gate = CompletableDeferred<Unit>()
        val hfRepo = PagedHuggingFaceRepo(gate) { listOf(hfModel(1L, "owner/stale")) }
        val loader = loaderWith(FakeModelRepository(), hfRepo)
        val filter = FilterState(selectedSources = setOf(ModelSource.HUGGING_FACE))

        val load = async { loader.loadPage(filter, cursor = null, limit = PAGE) }
        runCurrent()
        loader.resetPagination()
        gate.complete(Unit)

        assertFailsWith<CancellationException> { load.await() }
        assertNull(loader.multiSourceContinuation)
    }

    @Test
    fun multiSource_cancelledLoad_commitsNothing() = runTest {
        val gate = CompletableDeferred<Unit>()
        val hfRepo = PagedHuggingFaceRepo(gate) { listOf(hfModel(1L, "owner/stale")) }
        val loader = loaderWith(FakeModelRepository(), hfRepo)
        val filter = FilterState(selectedSources = setOf(ModelSource.HUGGING_FACE))

        val load = launch { loader.loadPage(filter, cursor = null, limit = PAGE) }
        runCurrent()
        load.cancel()
        gate.complete(Unit)
        load.join()

        assertNull(loader.multiSourceContinuation)
    }

    private fun hfModel(id: Long, nativeId: String, tags: List<String> = emptyList()) =
        testModel(id = id, tags = tags).copy(
            source = ModelSource.HUGGING_FACE,
            reference = ModelReference(ModelSource.HUGGING_FACE, nativeId, null),
        )

    private fun taModel(id: Long) = testModel(id = id).copy(
        source = ModelSource.TENSOR_ART,
        reference = ModelReference(ModelSource.TENSOR_ART, id.toString(), null),
    )

    private fun modelsWithIds(vararg ids: Long) = ids.map { testModel(id = it) }

    private fun modelWith(id: Long, downloads: Int = 100, rating: Double = 4.5) = testModel(
        id = id,
        stats = ModelStats(
            downloadCount = downloads,
            favoriteCount = 50,
            commentCount = 10,
            ratingCount = 20,
            rating = rating,
        ),
    )

    private suspend fun SearchPageLoader.loadIds(sort: SortOrder, cursor: String?, limit: Int): List<Long> =
        loadPage(FilterState(selectedSort = sort), cursor, limit).items.map { it.id }

    private companion object {
        // The loader requests at least PAGE_SIZE per provider, so a full raw page has this many models.
        const val PAGE = SearchPageLoader.PAGE_SIZE
        val ALL_SOURCES = setOf(ModelSource.CIVITAI, ModelSource.HUGGING_FACE, ModelSource.TENSOR_ART)
    }
}
