package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.dao.CollectionDao
import com.riox432.civitdeck.data.local.dao.TypeCount
import com.riox432.civitdeck.data.local.entity.CollectionEntity
import com.riox432.civitdeck.data.local.entity.CollectionModelEntity
import com.riox432.civitdeck.testModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("TooManyFunctions")
class FavoriteRepositoryImplTest {

    private class FakeCollectionDao : CollectionDao {
        val entries = mutableListOf<CollectionModelEntity>()
        val collections = mutableListOf<CollectionEntity>()
        private val flow = MutableStateFlow(0)

        override fun observeAllCollections(): Flow<List<CollectionEntity>> =
            flow.map { collections.toList() }

        override suspend fun insertCollection(collection: CollectionEntity): Long {
            collections.add(collection)
            flow.value++
            return collection.id
        }

        override suspend fun renameCollection(id: Long, name: String, updatedAt: Long) {}
        override suspend fun deleteCollection(id: Long) {}

        override fun observeEntriesByCollection(collectionId: Long): Flow<List<CollectionModelEntity>> =
            flow.map { entries.filter { it.collectionId == collectionId }.sortedByDescending { it.addedAt } }

        override suspend fun insertEntry(entry: CollectionModelEntity) {
            entries.removeAll { it.collectionId == entry.collectionId && it.modelId == entry.modelId }
            entries.add(entry)
            flow.value++
        }

        override suspend fun insertEntries(ents: List<CollectionModelEntity>) {
            ents.forEach { insertEntry(it) }
        }

        override suspend fun removeEntry(collectionId: Long, modelId: Long) {
            entries.removeAll { it.collectionId == collectionId && it.modelId == modelId }
            flow.value++
        }

        override suspend fun removeEntries(collectionId: Long, modelIds: List<Long>) {
            entries.removeAll { it.collectionId == collectionId && it.modelId in modelIds }
            flow.value++
        }

        override suspend fun getEntries(
            collectionId: Long,
            modelIds: List<Long>,
        ): List<CollectionModelEntity> =
            entries.filter { it.collectionId == collectionId && it.modelId in modelIds }

        override fun isFavorited(modelId: Long): Flow<Boolean> =
            flow.map { entries.any { it.collectionId == 1L && it.modelId == modelId } }

        override suspend fun isModelInCollection(collectionId: Long, modelId: Long): Boolean =
            entries.any { it.collectionId == collectionId && it.modelId == modelId }

        override suspend fun getAllFavoriteModelIds(): List<Long> =
            entries.filter { it.collectionId == 1L }.map { it.modelId }

        override suspend fun getFavoriteTypeCounts(): List<TypeCount> =
            entries.filter { it.collectionId == 1L }
                .groupBy { it.type }
                .map { (type, list) -> TypeCount(type, list.size) }

        override fun observeCollectionIdsForModel(modelId: Long): Flow<List<Long>> =
            flow.map { entries.filter { it.modelId == modelId }.map { it.collectionId } }

        override fun observeCollectionCount(collectionId: Long): Flow<Int> =
            flow.map { entries.count { it.collectionId == collectionId } }

        override fun observeCollectionThumbnail(collectionId: Long): Flow<String?> =
            flow.map { entries.filter { it.collectionId == collectionId }.maxByOrNull { it.addedAt }?.thumbnailUrl }
    }

    @Test
    fun toggleFavorite_adds_when_not_exists() = runTest {
        val dao = FakeCollectionDao()
        val repo = FavoriteRepositoryImpl(dao)
        val model = testModel(id = 1L)
        repo.toggleFavorite(model)
        assertEquals(1, dao.entries.size)
        assertEquals(1L, dao.entries[0].modelId)
    }

    @Test
    fun toggleFavorite_removes_when_exists() = runTest {
        val dao = FakeCollectionDao()
        dao.entries.add(testEntry(modelId = 1L))
        val repo = FavoriteRepositoryImpl(dao)
        repo.toggleFavorite(testModel(id = 1L))
        assertTrue(dao.entries.isEmpty())
    }

    @Test
    fun observeFavorites_maps_entities_to_domain() = runTest {
        val dao = FakeCollectionDao()
        dao.entries.add(testEntry(modelId = 1L, name = "Model A", type = "LORA", creatorName = "artist"))
        val repo = FavoriteRepositoryImpl(dao)
        val result = repo.observeFavorites().first()
        assertEquals(1, result.size)
        assertEquals("Model A", result[0].name)
        assertEquals("artist", result[0].creatorName)
    }

    @Test
    fun observeIsFavorite_returns_true_when_exists() = runTest {
        val dao = FakeCollectionDao()
        dao.entries.add(testEntry(modelId = 5L))
        val repo = FavoriteRepositoryImpl(dao)
        assertTrue(repo.observeIsFavorite(5L).first())
        assertFalse(repo.observeIsFavorite(99L).first())
    }

    @Test
    fun getAllFavoriteIds_returns_ids() = runTest {
        val dao = FakeCollectionDao()
        dao.entries.addAll(listOf(testEntry(modelId = 1L), testEntry(modelId = 2L)))
        val repo = FavoriteRepositoryImpl(dao)
        assertEquals(setOf(1L, 2L), repo.getAllFavoriteIds())
    }

    @Test
    fun getFavoriteTypeCounts_aggregates() = runTest {
        val dao = FakeCollectionDao()
        dao.entries.addAll(
            listOf(
                testEntry(modelId = 1L, type = "Checkpoint"),
                testEntry(modelId = 2L, type = "Checkpoint"),
                testEntry(modelId = 3L, type = "LORA"),
            ),
        )
        val repo = FavoriteRepositoryImpl(dao)
        val counts = repo.getFavoriteTypeCounts()
        assertEquals(2, counts["Checkpoint"])
        assertEquals(1, counts["LORA"])
    }

    private fun testEntry(
        modelId: Long = 1L,
        name: String = "Test",
        type: String = "Checkpoint",
        creatorName: String? = null,
    ) = CollectionModelEntity(
        collectionId = 1L,
        modelId = modelId,
        name = name,
        type = type,
        nsfw = false,
        thumbnailUrl = null,
        creatorName = creatorName,
        downloadCount = 0,
        favoriteCount = 0,
        rating = 0.0,
        addedAt = 1000L,
    )
}
