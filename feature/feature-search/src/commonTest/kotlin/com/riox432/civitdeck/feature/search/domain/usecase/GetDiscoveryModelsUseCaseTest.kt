package com.riox432.civitdeck.feature.search.domain.usecase

import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.testing.FakeModelRepository
import com.riox432.civitdeck.testing.testModel
import com.riox432.civitdeck.testing.testPaginatedResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [GetDiscoveryModelsUseCase] queries with the Newest sort order and unwraps the
 * paginated result into a plain list of models.
 */
class GetDiscoveryModelsUseCaseTest {

    @Test
    fun queriesNewestSortAndReturnsItems() = runTest {
        val models = listOf(testModel(id = 1L), testModel(id = 2L))
        val repo = FakeModelRepository(listOf(testPaginatedResult(items = models)))
        val useCase = GetDiscoveryModelsUseCase(repo)

        val result = useCase()

        // The unwrapped list is returned (not the PaginatedResult wrapper).
        assertEquals(models, result)
        assertEquals(SortOrder.Newest, repo.lastQuery!!.sort)
    }

    @Test
    fun forwardsCursorAndLimit() = runTest {
        val repo = FakeModelRepository(listOf(testPaginatedResult()))
        val useCase = GetDiscoveryModelsUseCase(repo)

        useCase(cursor = "next-page", limit = 50)

        val query = repo.lastQuery!!
        assertEquals("next-page", query.cursor)
        assertEquals(50, query.limit)
    }

    @Test
    fun usesDefaultLimitOf20() = runTest {
        val repo = FakeModelRepository(listOf(testPaginatedResult()))
        val useCase = GetDiscoveryModelsUseCase(repo)

        useCase()

        assertEquals(20, repo.lastQuery!!.limit)
    }
}
