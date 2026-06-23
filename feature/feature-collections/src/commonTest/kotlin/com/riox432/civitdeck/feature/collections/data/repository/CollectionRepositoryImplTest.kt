package com.riox432.civitdeck.feature.collections.data.repository

import com.riox432.civitdeck.data.local.dao.CollectionDao
import com.riox432.civitdeck.data.local.dao.CollectionWithCount
import com.riox432.civitdeck.data.local.dao.TypeCount
import com.riox432.civitdeck.data.local.entity.CollectionEntity
import com.riox432.civitdeck.data.local.entity.CollectionModelEntity
import com.riox432.civitdeck.domain.model.Creator
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.model.ModelStats
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.NsfwLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [CollectionRepositoryImpl]'s DAO delegation, the
 * [CollectionWithCount] -> [com.riox432.civitdeck.domain.model.ModelCollection]
 * and [CollectionModelEntity] -> summary mappings, the [Model] -> entry
 * projection, and the bulk move/remove flows.
 */
class CollectionRepositoryImplTest {

    private class FakeDao : CollectionDao {
        val collections = mutableListOf<CollectionEntity>()
        val entries = mutableListOf<CollectionModelEntity>()
        private var collectionIdCounter = 1L
        private val updates = MutableStateFlow(0)

        override fun observeAllCollections(): Flow<List<CollectionEntity>> =
            updates.map { collections.toList() }

        override fun observeAllCollectionsWithCount(): Flow<List<CollectionWithCount>> =
            updates.map {
                collections.map { c ->
                    val owned = entries.filter { it.collectionId == c.id }
                    CollectionWithCount(
                        id = c.id,
                        name = c.name,
                        isDefault = c.isDefault,
                        createdAt = c.createdAt,
                        updatedAt = c.updatedAt,
                        modelCount = owned.size,
                        thumbnailUrl = owned.maxByOrNull { it.addedAt }?.thumbnailUrl,
                    )
                }
            }

        override suspend fun insertCollection(collection: CollectionEntity): Long {
            val id = if (collection.id != 0L) collection.id else collectionIdCounter++
            collections.add(collection.copy(id = id))
            updates.value++
            return id
        }

        override suspend fun renameCollection(id: Long, name: String, updatedAt: Long): Int {
            val idx = collections.indexOfFirst { it.id == id && !it.isDefault }
            if (idx == -1) return 0
            collections[idx] = collections[idx].copy(name = name, updatedAt = updatedAt)
            updates.value++
            return 1
        }

        override suspend fun deleteCollection(id: Long): Int {
            val removed = collections.count { it.id == id && !it.isDefault }
            collections.removeAll { it.id == id && !it.isDefault }
            entries.removeAll { it.collectionId == id }
            updates.value++
            return removed
        }

        override fun observeEntriesByCollection(collectionId: Long): Flow<List<CollectionModelEntity>> =
            updates.map {
                entries.filter { it.collectionId == collectionId }
                    .sortedByDescending { it.addedAt }
            }

        override suspend fun insertEntry(entry: CollectionModelEntity) {
            entries.removeAll { it.collectionId == entry.collectionId && it.modelId == entry.modelId }
            entries.add(entry)
            updates.value++
        }

        override suspend fun insertEntries(entries: List<CollectionModelEntity>) {
            entries.forEach { insertEntry(it) }
        }

        override suspend fun removeEntry(collectionId: Long, modelId: Long): Int {
            val removed = entries.count { it.collectionId == collectionId && it.modelId == modelId }
            entries.removeAll { it.collectionId == collectionId && it.modelId == modelId }
            updates.value++
            return removed
        }

        override suspend fun removeEntries(collectionId: Long, modelIds: List<Long>): Int {
            val removed = entries.count { it.collectionId == collectionId && it.modelId in modelIds }
            entries.removeAll { it.collectionId == collectionId && it.modelId in modelIds }
            updates.value++
            return removed
        }

        override suspend fun getEntries(
            collectionId: Long,
            modelIds: List<Long>,
        ): List<CollectionModelEntity> =
            entries.filter { it.collectionId == collectionId && it.modelId in modelIds }

        override fun isFavorited(modelId: Long): Flow<Boolean> =
            updates.map { entries.any { it.collectionId == 1L && it.modelId == modelId } }

        override suspend fun isModelInCollection(collectionId: Long, modelId: Long): Boolean =
            entries.any { it.collectionId == collectionId && it.modelId == modelId }

        override suspend fun getAllFavoriteModelIds(): List<Long> =
            entries.filter { it.collectionId == 1L }.map { it.modelId }

        override suspend fun getFavoriteTypeCounts(): List<TypeCount> =
            entries.filter { it.collectionId == 1L }
                .groupingBy { it.type }.eachCount()
                .map { (type, cnt) -> TypeCount(type, cnt) }

        override suspend fun getAll(): List<CollectionEntity> = collections.sortedBy { it.id }

        override suspend fun getAllEntries(): List<CollectionModelEntity> = entries.toList()

        override suspend fun insertCollections(collections: List<CollectionEntity>) {
            collections.forEach { insertCollection(it) }
        }

        override suspend fun deleteAllEntries(): Int {
            val removed = entries.size
            entries.clear()
            updates.value++
            return removed
        }

        override suspend fun deleteAllNonDefault(): Int {
            val removed = collections.count { !it.isDefault }
            collections.removeAll { !it.isDefault }
            updates.value++
            return removed
        }

        override fun observeCollectionIdsForModel(modelId: Long): Flow<List<Long>> =
            updates.map { entries.filter { it.modelId == modelId }.map { it.collectionId } }

        override fun observeCollectionCount(collectionId: Long): Flow<Int> =
            updates.map { entries.count { it.collectionId == collectionId } }

        override fun observeCollectionThumbnail(collectionId: Long): Flow<String?> =
            updates.map {
                entries.filter { it.collectionId == collectionId }
                    .maxByOrNull { it.addedAt }?.thumbnailUrl
            }
    }

    private fun sampleModel(id: Long): Model = Model(
        id = id,
        name = "Model $id",
        description = null,
        type = ModelType.LORA,
        nsfw = false,
        tags = emptyList(),
        mode = null,
        creator = Creator(username = "bob", image = null, modelCount = null, link = null),
        stats = ModelStats(
            downloadCount = 10,
            favoriteCount = 5,
            commentCount = 0,
            ratingCount = 2,
            rating = 4.5,
        ),
        modelVersions = listOf(
            ModelVersion(
                id = 1,
                modelId = id,
                name = "v1",
                description = null,
                createdAt = "",
                baseModel = "SD 1.5",
                trainedWords = emptyList(),
                downloadUrl = "https://example.com/dl",
                files = emptyList(),
                images = listOf(
                    ModelImage(
                        url = "https://example.com/thumb.png",
                        nsfw = false,
                        nsfwLevel = NsfwLevel.None,
                        width = 100,
                        height = 100,
                        hash = null,
                        meta = null,
                    ),
                ),
                stats = null,
            ),
        ),
    )

    @Test
    fun observeCollections_maps_rows_with_counts_and_thumbnail() = runTest {
        val dao = FakeDao()
        dao.collections.add(CollectionEntity(id = 1, name = "Favorites", isDefault = true, createdAt = 1, updatedAt = 1))
        dao.entries.add(
            CollectionModelEntity(
                collectionId = 1, modelId = 9, name = "x", type = "LORA", nsfw = false,
                thumbnailUrl = "t.png", creatorName = "bob", downloadCount = 1,
                favoriteCount = 0, rating = 0.0, addedAt = 100,
            ),
        )
        val repo = CollectionRepositoryImpl(dao)

        val result = repo.observeCollections().first()

        assertEquals(1, result.size)
        assertEquals("Favorites", result.first().name)
        assertTrue(result.first().isDefault)
        assertEquals(1, result.first().modelCount)
        assertEquals("t.png", result.first().thumbnailUrl)
    }

    @Test
    fun createCollection_inserts_and_returns_new_id() = runTest {
        val dao = FakeDao()
        val repo = CollectionRepositoryImpl(dao)

        val id = repo.createCollection("My Set")

        assertEquals(1, dao.collections.size)
        assertEquals("My Set", dao.collections.first().name)
        assertEquals(id, dao.collections.first().id)
    }

    @Test
    fun renameCollection_updates_name_for_non_default() = runTest {
        val dao = FakeDao()
        dao.collections.add(CollectionEntity(id = 2, name = "Old", isDefault = false, createdAt = 1, updatedAt = 1))
        val repo = CollectionRepositoryImpl(dao)

        repo.renameCollection(id = 2, name = "New")

        assertEquals("New", dao.collections.first().name)
    }

    @Test
    fun deleteCollection_removes_non_default_collection() = runTest {
        val dao = FakeDao()
        dao.collections.add(CollectionEntity(id = 2, name = "Temp", isDefault = false, createdAt = 1, updatedAt = 1))
        val repo = CollectionRepositoryImpl(dao)

        repo.deleteCollection(2)

        assertTrue(dao.collections.isEmpty())
    }

    @Test
    fun addModelToCollection_projects_model_to_entry() = runTest {
        val dao = FakeDao()
        val repo = CollectionRepositoryImpl(dao)

        repo.addModelToCollection(collectionId = 1, model = sampleModel(42))

        val entry = dao.entries.single()
        assertEquals(1L, entry.collectionId)
        assertEquals(42L, entry.modelId)
        assertEquals("LORA", entry.type)
        assertEquals("bob", entry.creatorName)
        assertEquals("https://example.com/thumb.png", entry.thumbnailUrl)
        assertEquals(10, entry.downloadCount)
    }

    @Test
    fun observeModelsInCollection_maps_entries_to_summaries() = runTest {
        val dao = FakeDao()
        dao.entries.add(
            CollectionModelEntity(
                collectionId = 1, modelId = 7, name = "Anime", type = "Checkpoint", nsfw = true,
                thumbnailUrl = "a.png", creatorName = "alice", downloadCount = 99,
                favoriteCount = 3, rating = 4.0, addedAt = 200,
            ),
        )
        val repo = CollectionRepositoryImpl(dao)

        val summaries = repo.observeModelsInCollection(1).first()

        val summary = summaries.single()
        assertEquals(7L, summary.id)
        assertEquals("Anime", summary.name)
        assertEquals(ModelType.Checkpoint, summary.type)
        assertTrue(summary.nsfw)
        assertEquals("alice", summary.creatorName)
        assertEquals(200L, summary.favoritedAt)
    }

    @Test
    fun observeModelsInCollection_falls_back_to_checkpoint_for_unknown_type() = runTest {
        val dao = FakeDao()
        dao.entries.add(
            CollectionModelEntity(
                collectionId = 1, modelId = 7, name = "x", type = "NotAType", nsfw = false,
                thumbnailUrl = null, creatorName = null, downloadCount = 0,
                favoriteCount = 0, rating = 0.0, addedAt = 1,
            ),
        )
        val repo = CollectionRepositoryImpl(dao)

        val summary = repo.observeModelsInCollection(1).first().single()

        assertEquals(ModelType.Checkpoint, summary.type)
    }

    @Test
    fun removeModelFromCollection_removes_matching_entry() = runTest {
        val dao = FakeDao()
        dao.entries.add(
            CollectionModelEntity(
                collectionId = 1, modelId = 7, name = "x", type = "LORA", nsfw = false,
                thumbnailUrl = null, creatorName = null, downloadCount = 0,
                favoriteCount = 0, rating = 0.0, addedAt = 1,
            ),
        )
        val repo = CollectionRepositoryImpl(dao)

        repo.removeModelFromCollection(collectionId = 1, modelId = 7)

        assertTrue(dao.entries.isEmpty())
    }

    @Test
    fun bulkRemoveModels_removes_listed_ids_from_collection() = runTest {
        val dao = FakeDao()
        listOf(1L, 2L, 3L).forEach { mid ->
            dao.entries.add(
                CollectionModelEntity(
                    collectionId = 1, modelId = mid, name = "m$mid", type = "LORA", nsfw = false,
                    thumbnailUrl = null, creatorName = null, downloadCount = 0,
                    favoriteCount = 0, rating = 0.0, addedAt = mid,
                ),
            )
        }
        val repo = CollectionRepositoryImpl(dao)

        repo.bulkRemoveModels(collectionId = 1, modelIds = listOf(1L, 3L))

        assertEquals(listOf(2L), dao.entries.map { it.modelId })
    }

    @Test
    fun bulkMoveModels_moves_entries_to_target_collection() = runTest {
        val dao = FakeDao()
        dao.entries.add(
            CollectionModelEntity(
                collectionId = 1, modelId = 5, name = "m5", type = "LORA", nsfw = false,
                thumbnailUrl = "t.png", creatorName = "bob", downloadCount = 7,
                favoriteCount = 0, rating = 0.0, addedAt = 50,
            ),
        )
        val repo = CollectionRepositoryImpl(dao)

        repo.bulkMoveModels(fromCollectionId = 1, toCollectionId = 2, modelIds = listOf(5L))

        val moved = dao.entries.single()
        assertEquals(2L, moved.collectionId)
        assertEquals(5L, moved.modelId)
        assertEquals("bob", moved.creatorName) // preserved metadata
    }
}
