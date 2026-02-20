package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchHistoryUseCasesTest {

    private class FakeSearchHistoryRepository : SearchHistoryRepository {
        val searches = MutableStateFlow(listOf("kotlin", "anime"))
        var addedQuery: String? = null
        var clearCalled = false

        override fun observeRecentSearches(): Flow<List<String>> = searches
        override suspend fun addSearch(query: String) { addedQuery = query }
        override suspend fun deleteSearch(query: String) = error("not used")
        override suspend fun clearAll() { clearCalled = true }
    }

    private val repo = FakeSearchHistoryRepository()

    @Test
    fun observeSearchHistory_emits_list() = runTest {
        val useCase = ObserveSearchHistoryUseCase(repo)
        val result = useCase().first()
        assertEquals(listOf("kotlin", "anime"), result)
    }

    @Test
    fun addSearchHistory_delegates() = runTest {
        val useCase = AddSearchHistoryUseCase(repo)
        useCase("lora")
        assertEquals("lora", repo.addedQuery)
    }

    @Test
    fun clearSearchHistory_delegates() = runTest {
        val useCase = ClearSearchHistoryUseCase(repo)
        useCase()
        assertTrue(repo.clearCalled)
    }
}
