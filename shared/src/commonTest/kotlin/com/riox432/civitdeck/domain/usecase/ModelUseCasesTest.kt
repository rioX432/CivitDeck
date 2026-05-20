package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelSearchQuery
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.feature.creator.domain.usecase.GetCreatorModelsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.testModel
import com.riox432.civitdeck.testPaginatedResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelUseCasesTest {

    private val sampleModel = testModel()
    private val sampleResult = testPaginatedResult(items = listOf(sampleModel), nextCursor = "abc")

    private fun fakeRepository(
        modelsResult: PaginatedResult<Model> = sampleResult,
        modelDetail: Model = sampleModel,
    ) = object : ModelRepository {
        var lastQuery: ModelSearchQuery? = null

        override suspend fun getModels(query: ModelSearchQuery): PaginatedResult<Model> {
            lastQuery = query
            return modelsResult
        }

        override suspend fun getModel(id: Long): Model = modelDetail
        override suspend fun getModelVersion(id: Long): ModelVersion = error("not used")
        override suspend fun getModelVersionByHash(hash: String): ModelVersion = error("not used")
        override suspend fun getModelLicense(versionId: Long) = null
    }

    // --- GetModelsUseCase ---

    @Test
    fun getModels_returns_repository_result() = runTest {
        val repo = fakeRepository()
        val useCase = GetModelsUseCase(repo)
        val result = useCase()
        assertEquals(sampleResult, result)
    }

    @Test
    fun getModels_passes_all_parameters() = runTest {
        val repo = fakeRepository()
        val useCase = GetModelsUseCase(repo)
        useCase(
            query = "test",
            tag = "anime",
            type = ModelType.LORA,
            sort = SortOrder.Newest,
            period = TimePeriod.Week,
            cursor = "cursor123",
            limit = 20,
            nsfw = false,
        )
        val q = repo.lastQuery!!
        assertEquals("test", q.query)
        assertEquals("anime", q.tag)
        assertEquals(ModelType.LORA, q.type)
        assertEquals(SortOrder.Newest, q.sort)
        assertEquals(TimePeriod.Week, q.period)
        assertEquals("cursor123", q.cursor)
        assertEquals(20, q.limit)
        assertEquals(false, q.nsfw)
    }

    // --- GetModelDetailUseCase ---

    @Test
    fun getModelDetail_returns_model() = runTest {
        val model = testModel(id = 42L, name = "Detail Model")
        val repo = fakeRepository(modelDetail = model)
        val useCase = GetModelDetailUseCase(repo)
        val result = useCase(42L)
        assertEquals(42L, result.id)
        assertEquals("Detail Model", result.name)
    }

    // --- GetCreatorModelsUseCase ---

    @Test
    fun getCreatorModels_passes_username() = runTest {
        val repo = fakeRepository()
        val useCase = GetCreatorModelsUseCase(repo)
        useCase(username = "creator1", cursor = "cur", limit = 10)
        val q = repo.lastQuery!!
        assertEquals("creator1", q.username)
        assertEquals("cur", q.cursor)
        assertEquals(10, q.limit)
    }
}
