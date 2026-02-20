package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.dao.SearchHistoryDao
import com.riox432.civitdeck.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchHistoryRepositoryImplTest {

    private class FakeDao : SearchHistoryDao {
        val entities = mutableListOf<SearchHistoryEntity>()
        private var idCounter = 1L
        private val updates = MutableStateFlow(0)

        override fun observeRecent(limit: Int): Flow<List<SearchHistoryEntity>> =
            updates.map { entities.sortedByDescending { it.searchedAt }.take(limit) }

        override suspend fun deleteByQuery(query: String) {
            entities.removeAll { it.query == query }
            updates.value++
        }

        override suspend fun insert(entity: SearchHistoryEntity) {
            entities.add(entity.copy(id = idCounter++))
            updates.value++
        }

        override suspend fun clearAll() {
            entities.clear()
            updates.value++
        }

        override suspend fun deleteById(id: Long) {
            entities.removeAll { it.id == id }
            updates.value++
        }
    }

    @Test
    fun observeRecentSearches_maps_queries() = runTest {
        val dao = FakeDao()
        dao.entities.add(SearchHistoryEntity(id = 1, query = "anime", searchedAt = 1000L))
        dao.entities.add(SearchHistoryEntity(id = 2, query = "lora", searchedAt = 2000L))
        val repo = SearchHistoryRepositoryImpl(dao)
        val result = repo.observeRecentSearches().first()
        assertEquals(listOf("lora", "anime"), result) // ordered by searchedAt DESC
    }

    @Test
    fun addSearch_removes_duplicate_then_inserts() = runTest {
        val dao = FakeDao()
        dao.entities.add(SearchHistoryEntity(id = 1, query = "anime", searchedAt = 1000L))
        val repo = SearchHistoryRepositoryImpl(dao)
        repo.addSearch("anime")
        // old "anime" should be removed, new one inserted
        assertEquals(1, dao.entities.size)
        assertTrue(dao.entities[0].searchedAt > 1000L) // new timestamp
    }

    @Test
    fun addSearch_new_query_adds_to_list() = runTest {
        val dao = FakeDao()
        val repo = SearchHistoryRepositoryImpl(dao)
        repo.addSearch("checkpoint")
        assertEquals(1, dao.entities.size)
        assertEquals("checkpoint", dao.entities[0].query)
    }

    @Test
    fun clearAll_empties_history() = runTest {
        val dao = FakeDao()
        dao.entities.add(SearchHistoryEntity(id = 1, query = "a", searchedAt = 1000L))
        dao.entities.add(SearchHistoryEntity(id = 2, query = "b", searchedAt = 2000L))
        val repo = SearchHistoryRepositoryImpl(dao)
        repo.clearAll()
        assertTrue(dao.entities.isEmpty())
    }

    @Test
    fun deleteSearch_removes_by_query() = runTest {
        val dao = FakeDao()
        dao.entities.add(SearchHistoryEntity(id = 1, query = "anime", searchedAt = 1000L))
        dao.entities.add(SearchHistoryEntity(id = 2, query = "lora", searchedAt = 2000L))
        val repo = SearchHistoryRepositoryImpl(dao)
        repo.deleteSearch("anime")
        assertEquals(1, dao.entities.size)
        assertEquals("lora", dao.entities[0].query)
    }
}
