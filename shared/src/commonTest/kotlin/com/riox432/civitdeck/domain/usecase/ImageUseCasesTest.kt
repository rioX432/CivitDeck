package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.Image
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.ImageRepository
import com.riox432.civitdeck.testImage
import com.riox432.civitdeck.testPaginatedResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageUseCasesTest {

    private val images = listOf(testImage(id = 1L), testImage(id = 2L))
    private val result = testPaginatedResult(items = images, nextCursor = "next")

    private fun fakeRepository() = object : ImageRepository {
        var lastArgs: Map<String, Any?> = emptyMap()

        override suspend fun getImages(
            modelId: Long?,
            modelVersionId: Long?,
            username: String?,
            sort: SortOrder?,
            period: TimePeriod?,
            nsfwLevel: NsfwLevel?,
            limit: Int?,
            cursor: String?,
        ): PaginatedResult<Image> {
            lastArgs = mapOf(
                "modelId" to modelId, "modelVersionId" to modelVersionId,
                "username" to username, "sort" to sort, "period" to period,
                "nsfwLevel" to nsfwLevel, "limit" to limit, "cursor" to cursor,
            )
            return result
        }
    }

    @Test
    fun getImages_returns_result() = runTest {
        val repo = fakeRepository()
        val useCase = GetImagesUseCase(repo)
        val actual = useCase()
        assertEquals(2, actual.items.size)
        assertEquals("next", actual.metadata.nextCursor)
    }

    @Test
    fun getImages_passes_all_parameters() = runTest {
        val repo = fakeRepository()
        val useCase = GetImagesUseCase(repo)
        useCase(
            modelId = 10L,
            modelVersionId = 20L,
            username = "user1",
            sort = SortOrder.Newest,
            period = TimePeriod.Day,
            nsfwLevel = NsfwLevel.Soft,
            limit = 5,
            cursor = "cur",
        )
        assertEquals(10L, repo.lastArgs["modelId"])
        assertEquals(20L, repo.lastArgs["modelVersionId"])
        assertEquals("user1", repo.lastArgs["username"])
        assertEquals(SortOrder.Newest, repo.lastArgs["sort"])
        assertEquals(TimePeriod.Day, repo.lastArgs["period"])
        assertEquals(NsfwLevel.Soft, repo.lastArgs["nsfwLevel"])
        assertEquals(5, repo.lastArgs["limit"])
        assertEquals("cur", repo.lastArgs["cursor"])
    }
}
