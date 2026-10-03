package com.riox432.civitdeck.feature.search.domain.usecase

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.testing.FakeModelRepository
import com.riox432.civitdeck.testing.testModel
import com.riox432.civitdeck.testing.testPaginatedResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [GetDiscoveryModelsUseCase] queries with the Newest sort order, forwards the
 * `nsfw` flag, and returns the page together with its `nextCursor`.
 */
class GetDiscoveryModelsUseCaseTest {

    @Test
    fun queriesNewestSortAndReturnsItems() = runTest {
        val models = listOf(testModel(id = 1L), testModel(id = 2L))
        val repo = FakeModelRepository(listOf(testPaginatedResult(items = models)))
        val useCase = GetDiscoveryModelsUseCase(repo)

        val result = useCase(nsfw = false)

        assertEquals(models, result.items)
        assertEquals(SortOrder.Newest, repo.lastQuery!!.sort)
    }

    @Test
    fun exposesRepositoryNextCursor() = runTest {
        val repo = FakeModelRepository(listOf(testPaginatedResult<Model>(nextCursor = "c1")))
        val useCase = GetDiscoveryModelsUseCase(repo)

        val result = useCase(nsfw = false)

        assertEquals("c1", result.metadata.nextCursor)
    }

    @Test
    fun forwardsNsfwTrue() = runTest {
        val repo = FakeModelRepository(listOf(testPaginatedResult()))
        val useCase = GetDiscoveryModelsUseCase(repo)

        useCase(nsfw = true)

        assertEquals(true, repo.lastQuery!!.nsfw)
    }

    @Test
    fun forwardsNsfwFalse() = runTest {
        val repo = FakeModelRepository(listOf(testPaginatedResult()))
        val useCase = GetDiscoveryModelsUseCase(repo)

        useCase(nsfw = false)

        assertEquals(false, repo.lastQuery!!.nsfw)
    }

    @Test
    fun forwardsCursorAndLimit() = runTest {
        val repo = FakeModelRepository(listOf(testPaginatedResult()))
        val useCase = GetDiscoveryModelsUseCase(repo)

        useCase(nsfw = false, cursor = "next-page", limit = 50)

        val query = repo.lastQuery!!
        assertEquals("next-page", query.cursor)
        assertEquals(50, query.limit)
    }

    @Test
    fun usesDefaultLimitOf20() = runTest {
        val repo = FakeModelRepository(listOf(testPaginatedResult()))
        val useCase = GetDiscoveryModelsUseCase(repo)

        useCase(nsfw = false)

        assertEquals(20, repo.lastQuery!!.limit)
    }
}
