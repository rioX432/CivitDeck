package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.ModelRepository
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
        var lastGetModelsArgs: Map<String, Any?> = emptyMap()

        override suspend fun getModels(
            query: String?,
            tag: String?,
            type: ModelType?,
            sort: SortOrder?,
            period: TimePeriod?,
            baseModels: List<BaseModel>?,
            cursor: String?,
            limit: Int?,
            username: String?,
            nsfw: Boolean?,
        ): PaginatedResult<Model> {
            lastGetModelsArgs = mapOf(
                "query" to query, "tag" to tag, "type" to type,
                "sort" to sort, "period" to period, "baseModels" to baseModels,
                "cursor" to cursor, "limit" to limit, "username" to username,
                "nsfw" to nsfw,
            )
            return modelsResult
        }

        override suspend fun getModel(id: Long): Model = modelDetail
        override suspend fun getModelVersion(id: Long): ModelVersion = error("not used")
        override suspend fun getModelVersionByHash(hash: String): ModelVersion = error("not used")
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
        assertEquals("test", repo.lastGetModelsArgs["query"])
        assertEquals("anime", repo.lastGetModelsArgs["tag"])
        assertEquals(ModelType.LORA, repo.lastGetModelsArgs["type"])
        assertEquals(SortOrder.Newest, repo.lastGetModelsArgs["sort"])
        assertEquals(TimePeriod.Week, repo.lastGetModelsArgs["period"])
        assertEquals("cursor123", repo.lastGetModelsArgs["cursor"])
        assertEquals(20, repo.lastGetModelsArgs["limit"])
        assertEquals(false, repo.lastGetModelsArgs["nsfw"])
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
        assertEquals("creator1", repo.lastGetModelsArgs["username"])
        assertEquals("cur", repo.lastGetModelsArgs["cursor"])
        assertEquals(10, repo.lastGetModelsArgs["limit"])
    }
}
