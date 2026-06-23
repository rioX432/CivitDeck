package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.dao.BrowsingHistoryDao
import com.riox432.civitdeck.data.local.dao.CollectionDao
import com.riox432.civitdeck.data.local.dao.CollectionWithCount
import com.riox432.civitdeck.data.local.dao.DayCount
import com.riox432.civitdeck.data.local.dao.NameCount
import com.riox432.civitdeck.data.local.dao.SearchHistoryDao
import com.riox432.civitdeck.data.local.dao.SearchQueryCount
import com.riox432.civitdeck.data.local.dao.TypeCount
import com.riox432.civitdeck.data.local.entity.BrowsingHistoryEntity
import com.riox432.civitdeck.data.local.entity.CollectionEntity
import com.riox432.civitdeck.data.local.entity.CollectionModelEntity
import com.riox432.civitdeck.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [AnalyticsRepositoryImpl] verifying that browsing/collection/search DAO
 * aggregations are combined and mapped into a [BrowsingStats] domain object.
 */
class AnalyticsRepositoryImplTest {

    private class FakeBrowsingDao(
        private val total: Int,
        private val daily: List<DayCount>,
        private val types: List<NameCount>,
        private val creators: List<NameCount>,
        private val avgDuration: Long?,
    ) : BrowsingHistoryDao {
        override suspend fun count(): Int = total
        override suspend fun getDailyViewCounts(sinceMillis: Long): List<DayCount> = daily
        override suspend fun getTopModelTypes(limit: Int): List<NameCount> = types
        override suspend fun getTopCreators(limit: Int): List<NameCount> = creators
        override suspend fun getAverageViewDuration(): Long? = avgDuration

        override suspend fun insert(entity: BrowsingHistoryEntity) = Unit
        override suspend fun getRecent(limit: Int): List<BrowsingHistoryEntity> = emptyList()
        override fun observeRecent(limit: Int): Flow<List<BrowsingHistoryEntity>> = MutableStateFlow(emptyList())
        override suspend fun deleteById(id: Long) = Unit
        override suspend fun getRecentModelIds(limit: Int): List<Long> = emptyList()
        override suspend fun getAllModelIds(): List<Long> = emptyList()
        override suspend fun deleteAll(): Int = 0
        override suspend fun deleteOlderThan(cutoffMillis: Long): Int = 0
        override suspend fun deleteExcessEntries(maxCount: Int): Int = 0
        override suspend fun getTypeCountsSince(sinceMillis: Long, limit: Int): List<NameCount> = emptyList()
        override suspend fun getRecentSince(sinceMillis: Long, limit: Int): List<BrowsingHistoryEntity> = emptyList()
        override suspend fun getCreatorCountsSince(sinceMillis: Long, limit: Int): List<NameCount> = emptyList()
        override suspend fun updateDuration(id: Long, durationMs: Long) = Unit
        override suspend fun updateInteractionType(id: Long, interactionType: String) = Unit
        override suspend fun getLatestIdForModel(modelId: Long): Long? = null
        override suspend fun getInteractionCountByType(interactionType: String, sinceMillis: Long): Int = 0
    }

    private class FakeCollectionDao(
        private val favoriteCounts: List<TypeCount>,
    ) : CollectionDao {
        override suspend fun getFavoriteTypeCounts(): List<TypeCount> = favoriteCounts

        override fun observeAllCollections(): Flow<List<CollectionEntity>> = MutableStateFlow(emptyList())
        override fun observeAllCollectionsWithCount(): Flow<List<CollectionWithCount>> = MutableStateFlow(emptyList())
        override suspend fun insertCollection(collection: CollectionEntity): Long = 0
        override suspend fun renameCollection(id: Long, name: String, updatedAt: Long): Int = 0
        override suspend fun deleteCollection(id: Long): Int = 0
        override fun observeEntriesByCollection(collectionId: Long): Flow<List<CollectionModelEntity>> =
            MutableStateFlow(emptyList())
        override suspend fun insertEntry(entry: CollectionModelEntity) = Unit
        override suspend fun insertEntries(entries: List<CollectionModelEntity>) = Unit
        override suspend fun removeEntry(collectionId: Long, modelId: Long): Int = 0
        override suspend fun removeEntries(collectionId: Long, modelIds: List<Long>): Int = 0
        override suspend fun getEntries(collectionId: Long, modelIds: List<Long>): List<CollectionModelEntity> =
            emptyList()
        override fun isFavorited(modelId: Long): Flow<Boolean> = MutableStateFlow(false)
        override suspend fun isModelInCollection(collectionId: Long, modelId: Long): Boolean = false
        override suspend fun getAllFavoriteModelIds(): List<Long> = emptyList()
        override suspend fun getAll(): List<CollectionEntity> = emptyList()
        override suspend fun getAllEntries(): List<CollectionModelEntity> = emptyList()
        override suspend fun insertCollections(collections: List<CollectionEntity>) = Unit
        override suspend fun deleteAllEntries(): Int = 0
        override suspend fun deleteAllNonDefault(): Int = 0
        override fun observeCollectionIdsForModel(modelId: Long): Flow<List<Long>> = MutableStateFlow(emptyList())
        override fun observeCollectionCount(collectionId: Long): Flow<Int> = MutableStateFlow(0)
        override fun observeCollectionThumbnail(collectionId: Long): Flow<String?> = MutableStateFlow(null)
    }

    private class FakeSearchDao(
        private val total: Int,
        private val topQueries: List<SearchQueryCount>,
    ) : SearchHistoryDao {
        override suspend fun count(): Int = total
        override suspend fun getTopQueries(limit: Int): List<SearchQueryCount> = topQueries

        override fun observeRecent(limit: Int): Flow<List<SearchHistoryEntity>> = MutableStateFlow(emptyList())
        override suspend fun deleteByQuery(query: String): Int = 0
        override suspend fun insert(entity: SearchHistoryEntity) = Unit
        override suspend fun clearAll(): Int = 0
        override suspend fun deleteById(id: Long): Int = 0
    }

    @Test
    fun getBrowsingStats_aggregates_totals() = runTest {
        val repo = AnalyticsRepositoryImpl(
            browsingHistoryDao = FakeBrowsingDao(
                total = 12,
                daily = emptyList(),
                types = emptyList(),
                creators = emptyList(),
                avgDuration = 4200L,
            ),
            collectionDao = FakeCollectionDao(listOf(TypeCount("LORA", 3), TypeCount("Checkpoint", 2))),
            searchHistoryDao = FakeSearchDao(total = 5, topQueries = emptyList()),
        )
        val stats = repo.getBrowsingStats()
        assertEquals(12, stats.totalViews)
        assertEquals(5, stats.totalFavorites)
        assertEquals(5, stats.totalSearches)
        assertEquals(4200L, stats.averageViewDurationMs)
    }

    @Test
    fun getBrowsingStats_maps_category_aggregations() = runTest {
        val repo = AnalyticsRepositoryImpl(
            browsingHistoryDao = FakeBrowsingDao(
                total = 0,
                daily = listOf(DayCount(86400000L, 4)),
                types = listOf(NameCount("Checkpoint", 7)),
                creators = listOf(NameCount("artist", 3)),
                avgDuration = null,
            ),
            collectionDao = FakeCollectionDao(emptyList()),
            searchHistoryDao = FakeSearchDao(total = 0, topQueries = listOf(SearchQueryCount("anime", 9))),
        )
        val stats = repo.getBrowsingStats()
        assertEquals(86400000L, stats.dailyViewCounts.single().dayTimestamp)
        assertEquals(4, stats.dailyViewCounts.single().count)
        assertEquals("Checkpoint", stats.topModelTypes.single().name)
        assertEquals("artist", stats.topCreators.single().name)
        assertEquals("anime", stats.topSearchQueries.single().name)
    }

    @Test
    fun getBrowsingStats_sums_favorite_type_counts() = runTest {
        val repo = AnalyticsRepositoryImpl(
            browsingHistoryDao = FakeBrowsingDao(0, emptyList(), emptyList(), emptyList(), null),
            collectionDao = FakeCollectionDao(listOf(TypeCount("a", 4), TypeCount("b", 6))),
            searchHistoryDao = FakeSearchDao(0, emptyList()),
        )
        assertEquals(10, repo.getBrowsingStats().totalFavorites)
    }
}
