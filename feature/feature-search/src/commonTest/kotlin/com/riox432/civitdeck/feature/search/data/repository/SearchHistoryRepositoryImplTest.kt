package com.riox432.civitdeck.feature.search.data.repository

import com.riox432.civitdeck.data.local.dao.SearchHistoryDao
import com.riox432.civitdeck.data.local.dao.SearchQueryCount
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

        override suspend fun deleteByQuery(query: String): Int {
            val removed = entities.count { it.query == query }
            entities.removeAll { it.query == query }
            updates.value++
            return removed
        }

        override suspend fun insert(entity: SearchHistoryEntity) {
            entities.add(entity.copy(id = idCounter++))
            updates.value++
        }

        override suspend fun clearAll(): Int {
            val removed = entities.size
            entities.clear()
            updates.value++
            return removed
        }

        override suspend fun deleteById(id: Long): Int {
            val removed = entities.count { it.id == id }
            entities.removeAll { it.id == id }
            updates.value++
            return removed
        }

        override suspend fun count(): Int = entities.size

        override suspend fun getTopQueries(limit: Int): List<SearchQueryCount> =
            entities.groupingBy { it.query }.eachCount()
                .map { (name, cnt) -> SearchQueryCount(name, cnt) }
                .sortedByDescending { it.cnt }
                .take(limit)
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
