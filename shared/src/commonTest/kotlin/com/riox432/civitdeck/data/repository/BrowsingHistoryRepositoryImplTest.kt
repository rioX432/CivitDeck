package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.dao.BrowsingHistoryDao
import com.riox432.civitdeck.data.local.dao.DayCount
import com.riox432.civitdeck.data.local.dao.NameCount
import com.riox432.civitdeck.data.local.entity.BrowsingHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("TooManyFunctions")
class BrowsingHistoryRepositoryImplTest {

    private class FakeDao : BrowsingHistoryDao {
        val entities = mutableListOf<BrowsingHistoryEntity>()
        private var idCounter = 1L
        private val flow = MutableStateFlow<List<BrowsingHistoryEntity>>(emptyList())

        override suspend fun insert(entity: BrowsingHistoryEntity) {
            entities.add(entity.copy(id = idCounter++))
            flow.value = entities.toList()
        }

        override suspend fun getRecent(limit: Int): List<BrowsingHistoryEntity> =
            entities.sortedByDescending { it.viewedAt }.take(limit)

        override fun observeRecent(limit: Int): Flow<List<BrowsingHistoryEntity>> = flow

        override suspend fun deleteById(id: Long) {
            entities.removeAll { it.id == id }
            flow.value = entities.toList()
        }

        override suspend fun getRecentModelIds(limit: Int): List<Long> =
            entities.sortedByDescending { it.viewedAt }
                .map { it.modelId }
                .distinct()
                .take(limit)

        override suspend fun getAllModelIds(): List<Long> =
            entities.map { it.modelId }.distinct()

        override suspend fun count(): Int = entities.size

        override suspend fun deleteAll(): Int {
            val count = entities.size
            entities.clear()
            return count
        }

        override suspend fun getDailyViewCounts(sinceMillis: Long): List<DayCount> =
            entities.filter { it.viewedAt >= sinceMillis }
                .groupBy { (it.viewedAt / 86400000) * 86400000 }
                .map { (day, list) -> DayCount(day, list.size) }
                .sortedBy { it.day }

        override suspend fun getTopModelTypes(limit: Int): List<NameCount> =
            entities.groupBy { it.modelType }
                .map { (type, list) -> NameCount(type, list.size) }
                .sortedByDescending { it.cnt }
                .take(limit)

        override suspend fun getTopCreators(limit: Int): List<NameCount> =
            entities.filter { it.creatorName != null }
                .groupBy { it.creatorName!! }
                .map { (name, list) -> NameCount(name, list.size) }
                .sortedByDescending { it.cnt }
                .take(limit)

        override suspend fun deleteOlderThan(cutoffMillis: Long): Int {
            val before = entities.size
            entities.removeAll { it.viewedAt < cutoffMillis }
            return before - entities.size
        }

        override suspend fun deleteExcessEntries(maxCount: Int): Int {
            if (entities.size <= maxCount) return 0
            val sorted = entities.sortedByDescending { it.viewedAt }
            val toKeep = sorted.take(maxCount).toSet()
            val removed = entities.size - toKeep.size
            entities.retainAll(toKeep)
            return removed
        }

        override suspend fun getTypeCountsSince(sinceMillis: Long, limit: Int): List<NameCount> =
            entities.filter { it.viewedAt >= sinceMillis }
                .groupBy { it.modelType }
                .map { (type, list) -> NameCount(type, list.size) }
                .sortedByDescending { it.cnt }
                .take(limit)

        override suspend fun getRecentSince(sinceMillis: Long, limit: Int): List<BrowsingHistoryEntity> =
            entities.filter { it.viewedAt >= sinceMillis }
                .sortedByDescending { it.viewedAt }
                .take(limit)

        override suspend fun getCreatorCountsSince(sinceMillis: Long, limit: Int): List<NameCount> =
            entities.filter { it.creatorName != null && it.viewedAt >= sinceMillis }
                .groupBy { it.creatorName!! }
                .map { (name, list) -> NameCount(name, list.size) }
                .sortedByDescending { it.cnt }
                .take(limit)
    }

    @Test
    fun trackView_inserts_entity() = runTest {
        val dao = FakeDao()
        val repo = BrowsingHistoryRepositoryImpl(dao)
        repo.trackView(1L, "TestModel", "Checkpoint", "artist", null, listOf("anime"))
        assertEquals(1, dao.entities.size)
        assertEquals(1L, dao.entities[0].modelId)
        assertEquals("TestModel", dao.entities[0].modelName)
        assertEquals("Checkpoint", dao.entities[0].modelType)
        assertEquals("artist", dao.entities[0].creatorName)
        assertEquals("anime", dao.entities[0].tags)
    }

    @Test
    fun trackView_joins_tags_with_comma() = runTest {
        val dao = FakeDao()
        val repo = BrowsingHistoryRepositoryImpl(dao)
        repo.trackView(1L, "Test", "LORA", null, null, listOf("anime", "portrait", "girl"))
        assertEquals("anime,portrait,girl", dao.entities[0].tags)
    }

    @Test
    fun getRecentTypes_counts_by_type() = runTest {
        val dao = FakeDao()
        val repo = BrowsingHistoryRepositoryImpl(dao)
        repo.trackView(1L, "M1", "Checkpoint", null, null, emptyList())
        repo.trackView(2L, "M2", "Checkpoint", null, null, emptyList())
        repo.trackView(3L, "M3", "LORA", null, null, emptyList())

        val types = repo.getRecentTypes()
        assertEquals(2, types["Checkpoint"])
        assertEquals(1, types["LORA"])
    }

    @Test
    fun getRecentCreators_counts_by_creator() = runTest {
        val dao = FakeDao()
        val repo = BrowsingHistoryRepositoryImpl(dao)
        repo.trackView(1L, "M1", "Checkpoint", "artist1", null, emptyList())
        repo.trackView(2L, "M2", "LORA", "artist1", null, emptyList())
        repo.trackView(3L, "M3", "LORA", "artist2", null, emptyList())
        repo.trackView(4L, "M4", "LORA", null, null, emptyList())

        val creators = repo.getRecentCreators()
        assertEquals(2, creators["artist1"])
        assertEquals(1, creators["artist2"])
        assertEquals(2, creators.size)
    }

    @Test
    fun getRecentTags_counts_split_tags() = runTest {
        val dao = FakeDao()
        val repo = BrowsingHistoryRepositoryImpl(dao)
        repo.trackView(1L, "M1", "Checkpoint", null, null, listOf("anime", "girl"))
        repo.trackView(2L, "M2", "LORA", null, null, listOf("anime", "landscape"))

        val tags = repo.getRecentTags()
        assertEquals(2, tags["anime"])
        assertEquals(1, tags["girl"])
        assertEquals(1, tags["landscape"])
    }

    @Test
    fun getRecentTags_ignores_blank_tags() = runTest {
        val dao = FakeDao()
        val repo = BrowsingHistoryRepositoryImpl(dao)
        repo.trackView(1L, "M1", "Checkpoint", null, null, emptyList())

        val tags = repo.getRecentTags()
        assertTrue(tags.isEmpty())
    }

    @Test
    fun getAllViewedModelIds_returns_distinct_ids() = runTest {
        val dao = FakeDao()
        val repo = BrowsingHistoryRepositoryImpl(dao)
        repo.trackView(1L, "M1", "Checkpoint", null, null, emptyList())
        repo.trackView(1L, "M1", "Checkpoint", null, null, emptyList())
        repo.trackView(2L, "M2", "LORA", null, null, emptyList())

        val ids = repo.getAllViewedModelIds()
        assertEquals(setOf(1L, 2L), ids)
    }

    @Test
    fun clearAll_removes_everything() = runTest {
        val dao = FakeDao()
        val repo = BrowsingHistoryRepositoryImpl(dao)
        repo.trackView(1L, "M1", "Checkpoint", null, null, emptyList())
        repo.clearAll()
        assertTrue(dao.entities.isEmpty())
    }
}
