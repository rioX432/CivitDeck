package com.riox432.civitdeck.feature.search.presentation

import com.riox432.civitdeck.domain.model.Model
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
 */
class SearchPageLoaderTest {

    private class NoOpHuggingFaceRepo : HuggingFaceRepository {
        override suspend fun searchModels(query: String?, limit: Int, offset: Int): List<Model> =
            emptyList()
    }

    private class NoOpTensorArtRepo : TensorArtRepository {
        override suspend fun searchModels(query: String, page: Int, pageSize: Int): List<Model> =
            emptyList()
    }

    private fun loaderWith(modelRepo: FakeModelRepository): SearchPageLoader {
        val browsingRepo = FakeBrowsingHistoryRepository()
        return SearchPageLoader(
            getModelsUseCase = GetModelsUseCase(modelRepo),
            multiSourceSearchUseCase = MultiSourceSearchUseCase(
                modelRepository = modelRepo,
                huggingFaceRepository = NoOpHuggingFaceRepo(),
                tensorArtRepository = NoOpTensorArtRepo(),
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
        assertEquals(1, loader.multiSourcePage)
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
}
