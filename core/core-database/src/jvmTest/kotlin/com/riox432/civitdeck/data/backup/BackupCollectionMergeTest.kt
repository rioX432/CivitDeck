package com.riox432.civitdeck.data.backup

import androidx.room.Room
import com.riox432.civitdeck.data.local.CivitDeckDatabase
import com.riox432.civitdeck.data.local.entity.CollectionEntity
import com.riox432.civitdeck.data.local.entity.CollectionModelEntity
import com.riox432.civitdeck.data.local.getRoomDatabase
import com.riox432.civitdeck.domain.model.BackupCategory
import com.riox432.civitdeck.domain.model.RestoreStrategy
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Runs MERGE restores against a real in-memory Room database: the collection-entry
 * foreign key cascades on delete, which the fake DAOs in [BackupRepositoryImplTest]
 * cannot reproduce.
 */
class BackupCollectionMergeTest {

    private val db = getRoomDatabase(Room.inMemoryDatabaseBuilder<CivitDeckDatabase>())
    private val collectionDao = db.collectionDao()
    private val repo = BackupRepositoryImpl(
        collectionDaos = CollectionDaos(collectionDao),
        connectionDaos = ConnectionDaos(
            db.comfyUIConnectionDao(),
            db.sdWebUIConnectionDao(),
            db.externalServerConfigDao(),
        ),
        contentDaos = ContentDaos(
            db.savedPromptDao(),
            db.modelNoteDao(),
            db.personalTagDao(),
            db.savedSearchFilterDao(),
            db.followedCreatorDao(),
        ),
        preferenceDaos = PreferenceDaos(
            db.userPreferencesDao(),
            db.hiddenModelDao(),
            db.excludedTagDao(),
        ),
    )

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun merge_keeps_local_collection_whose_id_matches_a_backup_collection() = runTest {
        val localId = insertLocalCollection("Local", modelIds = listOf(1L, 2L, 3L))
        val backup = backupJson(
            collections = listOf(collectionDto(id = localId, name = "Backup")),
            models = listOf(modelDto(collectionId = localId, modelId = 10L)),
        )

        restoreMerge(backup)

        val local = collectionsNamed("Local").single()
        assertEquals(localId, local.id)
        assertEquals(setOf(1L, 2L, 3L), modelIdsIn(local.id))
        val restored = collectionsNamed("Backup").single()
        assertNotEquals(localId, restored.id)
        assertEquals(setOf(10L), modelIdsIn(restored.id))
    }

    @Test
    fun merge_adds_models_to_existing_collection_with_the_same_name() = runTest {
        val localId = insertLocalCollection("Local", modelIds = listOf(1L, 2L, 3L))
        val collectionCountBefore = collectionDao.getAll().size
        val backup = backupJson(
            collections = listOf(collectionDto(id = 42L, name = "Local")),
            models = listOf(modelDto(collectionId = 42L, modelId = 10L)),
        )

        restoreMerge(backup)

        assertEquals(collectionCountBefore, collectionDao.getAll().size)
        assertEquals(setOf(1L, 2L, 3L, 10L), modelIdsIn(localId))
    }

    @Test
    fun merge_restoring_the_same_backup_twice_keeps_collection_count() = runTest {
        insertLocalCollection("Local", modelIds = listOf(1L))
        val backup = backupJson(
            collections = listOf(
                collectionDto(id = FAVORITES_ID, name = "Favorites", isDefault = true),
                collectionDto(id = 2L, name = "Backup"),
            ),
            models = listOf(
                modelDto(collectionId = FAVORITES_ID, modelId = 20L),
                modelDto(collectionId = 2L, modelId = 10L),
            ),
        )

        restoreMerge(backup)
        val collectionCountAfterFirst = collectionDao.getAll().size
        restoreMerge(backup)

        assertEquals(collectionCountAfterFirst, collectionDao.getAll().size)
        assertEquals(setOf(20L), modelIdsIn(FAVORITES_ID))
        assertEquals(setOf(10L), modelIdsIn(collectionsNamed("Backup").single().id))
    }

    private suspend fun insertLocalCollection(name: String, modelIds: List<Long>): Long {
        val id = collectionDao.insertCollection(CollectionEntity(name = name, createdAt = 0L, updatedAt = 0L))
        collectionDao.insertEntries(modelIds.map { entryEntity(collectionId = id, modelId = it) })
        return id
    }

    private suspend fun restoreMerge(backupJson: String) {
        repo.restoreBackup(backupJson, RestoreStrategy.MERGE, setOf(BackupCategory.COLLECTIONS))
    }

    private suspend fun collectionsNamed(name: String) = collectionDao.getAll().filter { it.name == name }

    private suspend fun modelIdsIn(collectionId: Long): Set<Long> =
        collectionDao.getAllEntries().filter { it.collectionId == collectionId }.map { it.modelId }.toSet()

    private fun backupJson(collections: List<CollectionDto>, models: List<CollectionModelDto>): String =
        Json.encodeToString(
            BackupDto.serializer(),
            BackupDto(
                metadata = BackupMetadataDto(createdAt = 0L, categories = listOf(BackupCategory.COLLECTIONS.name)),
                collections = collections,
                collectionModels = models,
            ),
        )

    private fun collectionDto(id: Long, name: String, isDefault: Boolean = false) =
        CollectionDto(id = id, name = name, isDefault = isDefault, createdAt = 0L, updatedAt = 0L)

    private fun modelDto(collectionId: Long, modelId: Long) = CollectionModelDto(
        collectionId = collectionId,
        modelId = modelId,
        name = "model-$modelId",
        type = "LORA",
        nsfw = false,
        downloadCount = 0,
        favoriteCount = 0,
        rating = 0.0,
        addedAt = 0L,
    )

    private fun entryEntity(collectionId: Long, modelId: Long) = CollectionModelEntity(
        collectionId = collectionId,
        modelId = modelId,
        name = "model-$modelId",
        type = "LORA",
        nsfw = false,
        thumbnailUrl = null,
        creatorName = null,
        downloadCount = 0,
        favoriteCount = 0,
        rating = 0.0,
        addedAt = 0L,
    )

    private companion object {
        const val FAVORITES_ID = 1L
    }
}
