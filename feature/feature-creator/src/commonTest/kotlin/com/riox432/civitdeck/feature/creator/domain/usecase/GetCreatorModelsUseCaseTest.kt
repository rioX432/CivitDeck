package com.riox432.civitdeck.feature.creator.domain.usecase

import com.riox432.civitdeck.testing.FakeModelRepository
import com.riox432.civitdeck.testing.testModel
import com.riox432.civitdeck.testing.testPaginatedResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [GetCreatorModelsUseCase] bundles its parameters into a username-scoped
 * [com.riox432.civitdeck.domain.model.ModelSearchQuery] and returns the repository result.
 */
class GetCreatorModelsUseCaseTest {

    @Test
    fun buildsUsernameScopedQueryAndReturnsResult() = runTest {
        val models = listOf(testModel(id = 1L), testModel(id = 2L))
        val repo = FakeModelRepository(listOf(testPaginatedResult(items = models)))
        val useCase = GetCreatorModelsUseCase(repo)

        val result = useCase(username = "alice", nsfw = true, cursor = "c1", limit = 30)

        assertEquals(models, result.items)
        val query = repo.lastQuery!!
        assertEquals("alice", query.username)
        assertEquals("c1", query.cursor)
        assertEquals(30, query.limit)
        assertEquals(true, query.nsfw)
    }

    @Test
    fun forwardsNullCursorAndLimitByDefault() = runTest {
        val repo = FakeModelRepository(listOf(testPaginatedResult()))
        val useCase = GetCreatorModelsUseCase(repo)

        useCase(username = "bob", nsfw = false)

        val query = repo.lastQuery!!
        assertEquals("bob", query.username)
        assertEquals(null, query.cursor)
        assertEquals(null, query.limit)
        assertEquals(false, query.nsfw)
    }
}
