package com.riox432.civitdeck.feature.search.presentation

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.repository.HuggingFaceRepository
import com.riox432.civitdeck.domain.repository.TensorArtRepository
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase
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

/**
 * Covers the NSFW boundary in [SearchPageLoader.loadCivitaiPage]: the `/models`
 * `nsfw` parameter must always be sent explicitly (false for Off, true otherwise) —
 * omitting it makes the API strip non-PG images, leaving NSFW models thumbnail-less.
 * Finer level filtering (Soft) happens client-side via `filterNsfwImages`.
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
}
