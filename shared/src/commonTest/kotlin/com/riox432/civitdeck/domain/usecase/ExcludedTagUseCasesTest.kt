package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ExcludedTagRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExcludedTagUseCasesTest {

    private class FakeExcludedTagRepository : ExcludedTagRepository {
        val tags = mutableListOf("nsfw", "gore")
        var addedTag: String? = null
        var removedTag: String? = null

        override suspend fun getExcludedTags(): List<String> = tags.toList()
        override suspend fun addExcludedTag(tag: String) { addedTag = tag }
        override suspend fun removeExcludedTag(tag: String) { removedTag = tag }
    }

    private val repo = FakeExcludedTagRepository()

    @Test
    fun getExcludedTags_returns_list() = runTest {
        val useCase = GetExcludedTagsUseCase(repo)
        val result = useCase()
        assertEquals(listOf("nsfw", "gore"), result)
    }

    @Test
    fun addExcludedTag_delegates() = runTest {
        val useCase = AddExcludedTagUseCase(repo)
        useCase("violence")
        assertEquals("violence", repo.addedTag)
    }

    @Test
    fun removeExcludedTag_delegates() = runTest {
        val useCase = RemoveExcludedTagUseCase(repo)
        useCase("gore")
        assertEquals("gore", repo.removedTag)
    }
}
