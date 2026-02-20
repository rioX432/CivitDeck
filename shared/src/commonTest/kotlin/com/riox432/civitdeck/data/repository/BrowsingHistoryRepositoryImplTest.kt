package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.dao.BrowsingHistoryDao
import com.riox432.civitdeck.data.local.entity.BrowsingHistoryEntity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrowsingHistoryRepositoryImplTest {

    private class FakeDao : BrowsingHistoryDao {
        val entities = mutableListOf<BrowsingHistoryEntity>()
        private var idCounter = 1L

        override suspend fun insert(entity: BrowsingHistoryEntity) {
            entities.add(entity.copy(id = idCounter++))
        }

        override suspend fun getRecent(limit: Int): List<BrowsingHistoryEntity> =
            entities.sortedByDescending { it.viewedAt }.take(limit)

        override suspend fun getRecentModelIds(limit: Int): List<Long> =
            entities.sortedByDescending { it.viewedAt }
                .map { it.modelId }
                .distinct()
                .take(limit)

        override suspend fun getAllModelIds(): List<Long> =
            entities.map { it.modelId }.distinct()

        override suspend fun count(): Int = entities.size

        override suspend fun deleteAll() { entities.clear() }
    }

    @Test
    fun trackView_inserts_entity() = runTest {
        val dao = FakeDao()
        val repo = BrowsingHistoryRepositoryImpl(dao)
        repo.trackView(1L, "Checkpoint", "artist", listOf("anime"))
        assertEquals(1, dao.entities.size)
        assertEquals(1L, dao.entities[0].modelId)
        assertEquals("Checkpoint", dao.entities[0].modelType)
        assertEquals("artist", dao.entities[0].creatorName)
        assertEquals("anime", dao.entities[0].tags)
    }

    @Test
    fun trackView_joins_tags_with_comma() = runTest {
        val dao = FakeDao()
        val repo = BrowsingHistoryRepositoryImpl(dao)
        repo.trackView(1L, "LORA", null, listOf("anime", "portrait", "girl"))
        assertEquals("anime,portrait,girl", dao.entities[0].tags)
    }

    @Test
    fun getRecentTypes_counts_by_type() = runTest {
        val dao = FakeDao()
        val repo = BrowsingHistoryRepositoryImpl(dao)
        repo.trackView(1L, "Checkpoint", null, emptyList())
        repo.trackView(2L, "Checkpoint", null, emptyList())
        repo.trackView(3L, "LORA", null, emptyList())

        val types = repo.getRecentTypes()
        assertEquals(2, types["Checkpoint"])
        assertEquals(1, types["LORA"])
    }

    @Test
    fun getRecentCreators_counts_by_creator() = runTest {
        val dao = FakeDao()
        val repo = BrowsingHistoryRepositoryImpl(dao)
        repo.trackView(1L, "Checkpoint", "artist1", emptyList())
        repo.trackView(2L, "LORA", "artist1", emptyList())
        repo.trackView(3L, "LORA", "artist2", emptyList())
        repo.trackView(4L, "LORA", null, emptyList())

        val creators = repo.getRecentCreators()
        assertEquals(2, creators["artist1"])
        assertEquals(1, creators["artist2"])
        assertEquals(2, creators.size) // null creator excluded
    }

    @Test
    fun getRecentTags_counts_split_tags() = runTest {
        val dao = FakeDao()
        val repo = BrowsingHistoryRepositoryImpl(dao)
        repo.trackView(1L, "Checkpoint", null, listOf("anime", "girl"))
        repo.trackView(2L, "LORA", null, listOf("anime", "landscape"))

        val tags = repo.getRecentTags()
        assertEquals(2, tags["anime"])
        assertEquals(1, tags["girl"])
        assertEquals(1, tags["landscape"])
    }

    @Test
    fun getRecentTags_ignores_blank_tags() = runTest {
        val dao = FakeDao()
        val repo = BrowsingHistoryRepositoryImpl(dao)
        repo.trackView(1L, "Checkpoint", null, emptyList())

        val tags = repo.getRecentTags()
        assertTrue(tags.isEmpty())
    }

    @Test
    fun getAllViewedModelIds_returns_distinct_ids() = runTest {
        val dao = FakeDao()
        val repo = BrowsingHistoryRepositoryImpl(dao)
        repo.trackView(1L, "Checkpoint", null, emptyList())
        repo.trackView(1L, "Checkpoint", null, emptyList()) // duplicate
        repo.trackView(2L, "LORA", null, emptyList())

        val ids = repo.getAllViewedModelIds()
        assertEquals(setOf(1L, 2L), ids)
    }

    @Test
    fun clearAll_removes_everything() = runTest {
        val dao = FakeDao()
        val repo = BrowsingHistoryRepositoryImpl(dao)
        repo.trackView(1L, "Checkpoint", null, emptyList())
        repo.clearAll()
        assertTrue(dao.entities.isEmpty())
    }
}
