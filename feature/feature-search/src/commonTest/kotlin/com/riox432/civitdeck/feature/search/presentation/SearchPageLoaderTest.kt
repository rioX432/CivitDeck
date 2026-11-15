package com.riox432.civitdeck.feature.search.presentation

import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelReference
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.ModelStats
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.HuggingFaceRepository
import com.riox432.civitdeck.domain.repository.TensorArtRepository
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.QualityScoreCalculator
import com.riox432.civitdeck.feature.search.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.MultiSourceSearchUseCase
import com.riox432.civitdeck.testing.FakeBrowsingHistoryRepository
import com.riox432.civitdeck.testing.FakeModelRepository
import com.riox432.civitdeck.testing.testModel
import com.riox432.civitdeck.testing.testModelVersion
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
 *
 * Provider-policy tests cover the Civitai request in mixed search, the filters applied to external
 * results, unrated external previews, Civitai-only hidden/viewed IDs, and source notices.
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

    private class FailingHuggingFaceRepo : HuggingFaceRepository {
        override suspend fun searchModels(query: String?, limit: Int, offset: Int): List<Model> =
            throw IllegalStateException("HF down")
    }

    private fun loaderWith(
        modelRepo: FakeModelRepository,
        huggingFaceRepo: HuggingFaceRepository = NoOpHuggingFaceRepo(),
        tensorArtRepo: TensorArtRepository = NoOpTensorArtRepo(),
        hiddenIds: Set<Long> = emptySet(),
        viewedIds: Set<Long> = emptySet(),
        onSourceNoticesChanged: (List<SearchSourceNotice>) -> Unit = {},
    ): SearchPageLoader {
        val browsingRepo = FakeBrowsingHistoryRepository(viewedModelIds = viewedIds)
        return SearchPageLoader(
            getModelsUseCase = GetModelsUseCase(modelRepo),
            multiSourceSearchUseCase = MultiSourceSearchUseCase(
                modelRepository = modelRepo,
                huggingFaceRepository = huggingFaceRepo,
                tensorArtRepository = tensorArtRepo,
            ),
            getViewedModelIdsUseCase = GetViewedModelIdsUseCase(browsingRepo),
            hiddenModelIds = MutableStateFlow(hiddenIds),
            onSourceNoticesChanged = onSourceNoticesChanged,
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

    // ---- Provider policies ----

    @Test
    fun multiSource_civitaiRequest_hasSameExplicitFiltersAsCivitaiOnly() = runTest {
        val filter = FilterState(
            query = "cat",
            selectedType = ModelType.LORA,
            selectedSort = SortOrder.HighestRated,
            selectedPeriod = TimePeriod.Month,
            selectedBaseModels = setOf(BaseModel("SDXL 1.0")),
            nsfwFilterLevel = NsfwFilterLevel.Soft,
            includedTags = listOf("anime", "girl"),
        )
        val civitaiOnlyRepo = FakeModelRepository()
        loaderWith(civitaiOnlyRepo).loadPage(filter, cursor = null, limit = PAGE)
        val mixedRepo = FakeModelRepository()
        loaderWith(mixedRepo).loadPage(filter.copy(selectedSources = ALL_SOURCES), cursor = null, limit = PAGE)

        val expected = assertNotNull(civitaiOnlyRepo.lastQuery).copy(cursor = null, limit = null)
        assertEquals("anime", expected.tag)
        assertEquals(expected, assertNotNull(mixedRepo.lastQuery).copy(cursor = null, limit = null))
    }

    @Test
    fun multiSource_externalResults_needEveryIncludedTagAndMatchingType() = runTest {
        val hfRepo = PagedHuggingFaceRepo { offset ->
            if (offset > 0) {
                emptyList()
            } else {
                listOf(
                    hfModel(1L, "o/match", listOf("Anime", "girl"), ModelType.LORA),
                    hfModel(2L, "o/one-tag", listOf("anime"), ModelType.LORA),
                    hfModel(3L, "o/wrong-type", listOf("anime", "girl"), ModelType.Checkpoint),
                    hfModel(4L, "o/excluded", listOf("anime", "girl", "Horror"), ModelType.LORA),
                )
            }
        }
        val loader = loaderWith(FakeModelRepository(), hfRepo)
        val filter = FilterState(
            selectedType = ModelType.LORA,
            includedTags = listOf("anime", "girl"),
            excludedTags = listOf("horror"),
            nsfwFilterLevel = NsfwFilterLevel.All,
            selectedSources = setOf(ModelSource.HUGGING_FACE),
        )

        val result = loader.loadPage(filter, cursor = null, limit = PAGE)

        assertEquals(listOf("o/match"), result.items.map { it.reference.nativeId })
    }

    @Test
    fun multiSource_unratedExternalPreviews_hiddenUnderOffAndSoft_keptUnderAll() = runTest {
        for (level in NsfwFilterLevel.entries) {
            val loader = loaderWith(
                FakeModelRepository(),
                PagedHuggingFaceRepo { offset -> if (offset > 0) emptyList() else listOf(hfModel(1L, "o/x")) },
                FixedTensorArtRepo(listOf(taModel(2L))),
            )
            val filter = FilterState(
                nsfwFilterLevel = level,
                selectedSources = setOf(ModelSource.HUGGING_FACE, ModelSource.TENSOR_ART),
            )

            val items = loader.loadPage(filter, cursor = null, limit = PAGE).items

            assertEquals(listOf(ModelSource.HUGGING_FACE, ModelSource.TENSOR_ART), items.map { it.source }, "$level")
            val tensorArtImages = items.last().modelVersions.sumOf { it.images.size }
            assertEquals(if (level == NsfwFilterLevel.All) 1 else 0, tensorArtImages, "$level")
        }
    }

    @Test
    fun multiSource_civitaiImageRulesStillApply() = runTest {
        val explicitOnly = testModel(id = 1L, modelVersions = listOf(testModelVersion(nsfwLevel = NsfwLevel.X)))
        val modelRepo = FakeModelRepository(
            pages = listOf(testPaginatedResult(items = listOf(explicitOnly, testModel(id = 2L)))),
        )
        val loader = loaderWith(modelRepo, tensorArtRepo = FixedTensorArtRepo(listOf(taModel(3L))))
        val filter = FilterState(
            nsfwFilterLevel = NsfwFilterLevel.Off,
            selectedSources = setOf(ModelSource.CIVITAI, ModelSource.TENSOR_ART),
        )

        val items = loader.loadPage(filter, cursor = null, limit = PAGE).items

        assertEquals(listOf("CIVITAI:2", "TENSOR_ART:3"), items.map { it.reference.stableKey })
    }

    @Test
    fun multiSource_civitaiQualitySort_reordersOnlyCivitaiModels() = runTest {
        val low = modelWith(id = 1L, rating = 2.0)
        val high = modelWith(id = 2L, rating = 5.0)
        val modelRepo = FakeModelRepository(pages = listOf(testPaginatedResult(items = listOf(low, high))))
        val loader = loaderWith(modelRepo, tensorArtRepo = FixedTensorArtRepo(listOf(taModel(3L))))
        val filter = FilterState(
            selectedSort = SortOrder.Quality,
            nsfwFilterLevel = NsfwFilterLevel.All,
            selectedSources = setOf(ModelSource.CIVITAI, ModelSource.TENSOR_ART),
        )

        val items = loader.loadPage(filter, cursor = null, limit = PAGE).items

        assertTrue(QualityScoreCalculator.calculate(high.stats) > QualityScoreCalculator.calculate(low.stats))
        assertEquals(listOf("CIVITAI:2", "CIVITAI:1", "TENSOR_ART:3"), items.map { it.reference.stableKey })
    }

    @Test
    fun multiSource_hiddenAndViewedCivitaiIds_neverHideExternalModelsWithSameNumber() = runTest {
        val modelRepo = FakeModelRepository(pages = listOf(testPaginatedResult(items = modelsWithIds(5L, 6L, 7L))))
        val loader = loaderWith(
            modelRepo,
            PagedHuggingFaceRepo { offset -> if (offset > 0) emptyList() else listOf(hfModel(5L, "o/five")) },
            FixedTensorArtRepo(listOf(taModel(6L))),
            hiddenIds = setOf(5L),
            viewedIds = setOf(6L),
        )
        val filter = FilterState(
            isFreshFindEnabled = true,
            nsfwFilterLevel = NsfwFilterLevel.All,
            selectedSources = ALL_SOURCES,
        )

        val items = loader.loadPage(filter, cursor = null, limit = PAGE).items

        assertEquals(listOf("CIVITAI:7", "HUGGING_FACE:o/five", "TENSOR_ART:6"), items.map { it.reference.stableKey })
    }

    @Test
    fun multiSource_baseModel_checkedOnTensorArt_andReportedUnsupportedOnHuggingFace() = runTest {
        val loader = loaderWith(
            FakeModelRepository(),
            PagedHuggingFaceRepo { offset -> if (offset > 0) emptyList() else listOf(hfModel(10L, "o/any")) },
            FixedTensorArtRepo(listOf(taModel(1L, "sdxl 1.0"), taModel(2L, "SD 1.5"), taModel(3L, null))),
        )
        val filter = FilterState(
            selectedBaseModels = setOf(BaseModel("SDXL 1.0")),
            nsfwFilterLevel = NsfwFilterLevel.All,
            selectedSources = setOf(ModelSource.HUGGING_FACE, ModelSource.TENSOR_ART),
        )

        val items = loader.loadPage(filter, cursor = null, limit = PAGE).items

        assertEquals(listOf("HUGGING_FACE:o/any", "TENSOR_ART:1"), items.map { it.reference.stableKey })
        assertEquals(
            listOf(SearchSourceNotice(ModelSource.HUGGING_FACE, SearchSourceNoticeReason.UNSUPPORTED_BASE_MODEL)),
            loader.sourceNotices,
        )
    }

    @Test
    fun multiSource_everyUnsupportedActiveFilter_producesNoticePerExternalSource() = runTest {
        val published = mutableListOf<List<SearchSourceNotice>>()
        val loader = loaderWith(FakeModelRepository(), onSourceNoticesChanged = { published += it })
        val filter = FilterState(
            selectedSort = SortOrder.Newest,
            selectedPeriod = TimePeriod.Week,
            selectedBaseModels = setOf(BaseModel("SDXL 1.0")),
            nsfwFilterLevel = NsfwFilterLevel.Soft,
            isFreshFindEnabled = true,
            isQualityFilterEnabled = true,
            qualityThreshold = 50,
            selectedSources = ALL_SOURCES,
        )

        loader.loadPage(filter, cursor = null, limit = PAGE)

        val shared = listOf(
            SearchSourceNoticeReason.UNSUPPORTED_SORT,
            SearchSourceNoticeReason.UNSUPPORTED_PERIOD,
            SearchSourceNoticeReason.UNSUPPORTED_FRESH_FIND,
            SearchSourceNoticeReason.UNSUPPORTED_QUALITY_FILTER,
            SearchSourceNoticeReason.UNRATED_PREVIEWS_HIDDEN,
        )
        val huggingFace = (shared + SearchSourceNoticeReason.UNSUPPORTED_BASE_MODEL).sorted()
        assertEquals(
            huggingFace.map { SearchSourceNotice(ModelSource.HUGGING_FACE, it) } +
                shared.map { SearchSourceNotice(ModelSource.TENSOR_ART, it) },
            loader.sourceNotices,
        )
        assertEquals(listOf(loader.sourceNotices), published)
    }

    @Test
    fun multiSource_defaultFiltersUnderAll_produceNoNotices() = runTest {
        val published = mutableListOf<List<SearchSourceNotice>>()
        val loader = loaderWith(FakeModelRepository(), onSourceNoticesChanged = { published += it })
        val filter = FilterState(nsfwFilterLevel = NsfwFilterLevel.All, selectedSources = ALL_SOURCES)

        loader.loadPage(filter, cursor = null, limit = PAGE)

        assertTrue(loader.sourceNotices.isEmpty())
        assertTrue(published.isEmpty())
    }

    @Test
    fun multiSource_failingSource_addsNotice_whileOtherModelsRemain() = runTest {
        val loader = loaderWith(
            FakeModelRepository(),
            FailingHuggingFaceRepo(),
            FixedTensorArtRepo(listOf(taModel(1L))),
        )
        val filter = FilterState(
            nsfwFilterLevel = NsfwFilterLevel.All,
            selectedSources = setOf(ModelSource.HUGGING_FACE, ModelSource.TENSOR_ART),
        )

        val items = loader.loadPage(filter, cursor = null, limit = PAGE).items

        assertEquals(listOf("TENSOR_ART:1"), items.map { it.reference.stableKey })
        assertEquals(
            listOf(SearchSourceNotice(ModelSource.HUGGING_FACE, SearchSourceNoticeReason.SOURCE_FAILED)),
            loader.sourceNotices,
        )
    }

    @Test
    fun sourceNotices_clearedOnReset_andNotSetByCivitaiOnlySearch() = runTest {
        val published = mutableListOf<List<SearchSourceNotice>>()
        val loader = loaderWith(
            FakeModelRepository(),
            FailingHuggingFaceRepo(),
            FixedTensorArtRepo(listOf(taModel(1L))),
            onSourceNoticesChanged = { published += it },
        )
        val mixed = FilterState(selectedSources = setOf(ModelSource.HUGGING_FACE, ModelSource.TENSOR_ART))
        loader.loadPage(mixed, cursor = null, limit = PAGE)
        assertTrue(loader.sourceNotices.isNotEmpty())

        loader.resetPagination()
        assertEquals(emptyList<SearchSourceNotice>(), published.last())
        loader.loadPage(FilterState(selectedSort = SortOrder.Newest), cursor = null, limit = PAGE)

        assertTrue(loader.sourceNotices.isEmpty())
        assertEquals(2, published.size)
    }

    @Test
    fun multiSource_totalFailure_throws_withoutPublishingNotices() = runTest {
        val published = mutableListOf<List<SearchSourceNotice>>()
        val loader = loaderWith(
            FakeModelRepository(),
            FailingHuggingFaceRepo(),
            onSourceNoticesChanged = { published += it },
        )
        val filter = FilterState(selectedSources = setOf(ModelSource.HUGGING_FACE))

        val outcome = runCatching { loader.loadPage(filter, cursor = null, limit = PAGE) }

        assertTrue(outcome.isFailure)
        assertTrue(published.isEmpty())
        assertTrue(loader.sourceNotices.isEmpty())
    }

    private fun hfModel(
        id: Long,
        nativeId: String,
        tags: List<String> = emptyList(),
        type: ModelType = ModelType.Checkpoint,
        versions: List<ModelVersion> = emptyList(),
    ) = testModel(id = id, tags = tags, type = type, modelVersions = versions).copy(
        source = ModelSource.HUGGING_FACE,
        reference = ModelReference(ModelSource.HUGGING_FACE, nativeId, null),
    )

    // TensorArt's mapper carries the base model on a single synthetic version.
    private fun taModel(id: Long, baseModel: String? = "SD 1.5") = testModel(
        id = id,
        modelVersions = listOf(testModelVersion(modelId = id).copy(baseModel = baseModel)),
    ).copy(
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
