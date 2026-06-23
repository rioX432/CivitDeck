package com.riox432.civitdeck.data.backup

import com.riox432.civitdeck.data.local.dao.CollectionDao
import com.riox432.civitdeck.data.local.dao.CollectionWithCount
import com.riox432.civitdeck.data.local.dao.ComfyUIConnectionDao
import com.riox432.civitdeck.data.local.dao.ExcludedTagDao
import com.riox432.civitdeck.data.local.dao.ExternalServerConfigDao
import com.riox432.civitdeck.data.local.dao.FollowedCreatorDao
import com.riox432.civitdeck.data.local.dao.HiddenModelDao
import com.riox432.civitdeck.data.local.dao.ModelNoteDao
import com.riox432.civitdeck.data.local.dao.PersonalTagDao
import com.riox432.civitdeck.data.local.dao.SDWebUIConnectionDao
import com.riox432.civitdeck.data.local.dao.SavedPromptDao
import com.riox432.civitdeck.data.local.dao.SavedSearchFilterDao
import com.riox432.civitdeck.data.local.dao.TypeCount
import com.riox432.civitdeck.data.local.dao.UserPreferencesDao
import com.riox432.civitdeck.data.local.entity.CollectionEntity
import com.riox432.civitdeck.data.local.entity.CollectionModelEntity
import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.data.local.entity.ExcludedTagEntity
import com.riox432.civitdeck.data.local.entity.ExternalServerConfigEntity
import com.riox432.civitdeck.data.local.entity.FollowedCreatorEntity
import com.riox432.civitdeck.data.local.entity.HiddenModelEntity
import com.riox432.civitdeck.data.local.entity.ModelNoteEntity
import com.riox432.civitdeck.data.local.entity.PersonalTagEntity
import com.riox432.civitdeck.data.local.entity.SDWebUIConnectionEntity
import com.riox432.civitdeck.data.local.entity.SavedPromptEntity
import com.riox432.civitdeck.data.local.entity.SavedSearchFilterEntity
import com.riox432.civitdeck.data.local.entity.UserPreferencesEntity
import com.riox432.civitdeck.domain.model.BackupCategory
import com.riox432.civitdeck.domain.model.RestoreStrategy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [BackupRepositoryImpl] covering selective category export, metadata
 * category parsing, and a MERGE/OVERWRITE restore round-trip for the notes & tags subset.
 * Connection/preference DAOs are stubbed since those categories are not exercised here.
 */
class BackupRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeModelNoteDao : ModelNoteDao {
        val notes = mutableListOf<ModelNoteEntity>()
        override suspend fun getAll(): List<ModelNoteEntity> = notes.toList()
        override suspend fun insertAll(entities: List<ModelNoteEntity>) { notes.addAll(entities) }
        override suspend fun deleteAll(): Int { val c = notes.size; notes.clear(); return c }
        override fun observeByModelId(modelId: Long): Flow<ModelNoteEntity?> = MutableStateFlow(null)
        override suspend fun getByModelId(modelId: Long): ModelNoteEntity? = null
        override suspend fun upsert(entity: ModelNoteEntity) = Unit
        override suspend fun deleteByModelId(modelId: Long): Int = 0
    }

    private class FakePersonalTagDao : PersonalTagDao {
        val tags = mutableListOf<PersonalTagEntity>()
        override suspend fun getAll(): List<PersonalTagEntity> = tags.toList()
        override suspend fun insertAll(entities: List<PersonalTagEntity>) { tags.addAll(entities) }
        override suspend fun deleteAll(): Int { val c = tags.size; tags.clear(); return c }
        override fun observeByModelId(modelId: Long): Flow<List<PersonalTagEntity>> = MutableStateFlow(emptyList())
        override suspend fun insert(entity: PersonalTagEntity) = Unit
        override suspend fun delete(modelId: Long, tag: String): Int = 0
        override suspend fun getAllDistinctTags(): List<String> = emptyList()
        override suspend fun getModelIdsByTag(tag: String): List<Long> = emptyList()
    }

    private class FakeCollectionDao : CollectionDao {
        val collections = mutableListOf<CollectionEntity>()
        val entries = mutableListOf<CollectionModelEntity>()
        override suspend fun getAll(): List<CollectionEntity> = collections.toList()
        override suspend fun getAllEntries(): List<CollectionModelEntity> = entries.toList()
        override suspend fun insertCollections(collections: List<CollectionEntity>) { this.collections.addAll(collections) }
        override suspend fun insertEntries(entries: List<CollectionModelEntity>) { this.entries.addAll(entries) }
        override suspend fun deleteAllEntries(): Int { val c = entries.size; entries.clear(); return c }
        override suspend fun deleteAllNonDefault(): Int {
            val c = collections.count { !it.isDefault }; collections.removeAll { !it.isDefault }; return c
        }
        override suspend fun getFavoriteTypeCounts(): List<TypeCount> = emptyList()
        override fun observeAllCollections(): Flow<List<CollectionEntity>> = MutableStateFlow(emptyList())
        override fun observeAllCollectionsWithCount(): Flow<List<CollectionWithCount>> = MutableStateFlow(emptyList())
        override suspend fun insertCollection(collection: CollectionEntity): Long = 0
        override suspend fun renameCollection(id: Long, name: String, updatedAt: Long): Int = 0
        override suspend fun deleteCollection(id: Long): Int = 0
        override fun observeEntriesByCollection(collectionId: Long): Flow<List<CollectionModelEntity>> =
            MutableStateFlow(emptyList())
        override suspend fun insertEntry(entry: CollectionModelEntity) = Unit
        override suspend fun removeEntry(collectionId: Long, modelId: Long): Int = 0
        override suspend fun removeEntries(collectionId: Long, modelIds: List<Long>): Int = 0
        override suspend fun getEntries(collectionId: Long, modelIds: List<Long>): List<CollectionModelEntity> =
            emptyList()
        override fun isFavorited(modelId: Long): Flow<Boolean> = MutableStateFlow(false)
        override suspend fun isModelInCollection(collectionId: Long, modelId: Long): Boolean = false
        override suspend fun getAllFavoriteModelIds(): List<Long> = emptyList()
        override fun observeCollectionIdsForModel(modelId: Long): Flow<List<Long>> = MutableStateFlow(emptyList())
        override fun observeCollectionCount(collectionId: Long): Flow<Int> = MutableStateFlow(0)
        override fun observeCollectionThumbnail(collectionId: Long): Flow<String?> = MutableStateFlow(null)
    }

    private fun buildRepo(
        noteDao: FakeModelNoteDao = FakeModelNoteDao(),
        tagDao: FakePersonalTagDao = FakePersonalTagDao(),
        collectionDao: FakeCollectionDao = FakeCollectionDao(),
    ) = BackupRepositoryImpl(
        collectionDaos = CollectionDaos(collectionDao),
        connectionDaos = ConnectionDaos(StubComfyDao, StubSdDao, StubExternalDao),
        contentDaos = ContentDaos(StubPromptDao, noteDao, tagDao, StubFilterDao, StubFollowDao),
        preferenceDaos = PreferenceDaos(StubPrefsDao, StubHiddenDao, StubExcludedDao),
    )

    @Test
    fun createBackup_includes_only_selected_categories() = runTest {
        val noteDao = FakeModelNoteDao().apply { notes.add(ModelNoteEntity(modelId = 1L, noteText = "n", createdAt = 1L, updatedAt = 1L)) }
        val tagDao = FakePersonalTagDao().apply { tags.add(PersonalTagEntity(modelId = 1L, tag = "anime", addedAt = 1L)) }
        val repo = buildRepo(noteDao = noteDao, tagDao = tagDao)
        val jsonStr = repo.createBackup(setOf(BackupCategory.NOTES))
        val dto = json.decodeFromString(BackupDto.serializer(), jsonStr)
        assertEquals(1, dto.modelNotes?.size)
        assertEquals(null, dto.personalTags) // TAGS not selected
    }

    @Test
    fun parseBackupCategories_reads_metadata() = runTest {
        val repo = buildRepo()
        val jsonStr = repo.createBackup(setOf(BackupCategory.NOTES, BackupCategory.TAGS))
        assertEquals(setOf(BackupCategory.NOTES, BackupCategory.TAGS), repo.parseBackupCategories(jsonStr))
    }

    @Test
    fun restoreBackup_merge_appends_notes_and_tags() = runTest {
        val sourceNotes = FakeModelNoteDao().apply { notes.add(ModelNoteEntity(modelId = 2L, noteText = "x", createdAt = 1L, updatedAt = 1L)) }
        val sourceTags = FakePersonalTagDao().apply { tags.add(PersonalTagEntity(modelId = 2L, tag = "girl", addedAt = 1L)) }
        val backup = buildRepo(noteDao = sourceNotes, tagDao = sourceTags)
            .createBackup(setOf(BackupCategory.NOTES, BackupCategory.TAGS))

        val targetNotes = FakeModelNoteDao().apply { notes.add(ModelNoteEntity(modelId = 9L, noteText = "old", createdAt = 0L, updatedAt = 0L)) }
        val targetTags = FakePersonalTagDao()
        val targetRepo = buildRepo(noteDao = targetNotes, tagDao = targetTags)
        targetRepo.restoreBackup(backup, RestoreStrategy.MERGE, setOf(BackupCategory.NOTES, BackupCategory.TAGS))

        assertEquals(2, targetNotes.notes.size) // existing + restored
        assertEquals(listOf("girl"), targetTags.tags.map { it.tag })
    }

    @Test
    fun restoreBackup_overwrite_clears_existing_notes() = runTest {
        val sourceNotes = FakeModelNoteDao().apply { notes.add(ModelNoteEntity(modelId = 2L, noteText = "new", createdAt = 1L, updatedAt = 1L)) }
        val backup = buildRepo(noteDao = sourceNotes).createBackup(setOf(BackupCategory.NOTES))

        val targetNotes = FakeModelNoteDao().apply { notes.add(ModelNoteEntity(modelId = 9L, noteText = "old", createdAt = 0L, updatedAt = 0L)) }
        val targetRepo = buildRepo(noteDao = targetNotes)
        targetRepo.restoreBackup(backup, RestoreStrategy.OVERWRITE, setOf(BackupCategory.NOTES))

        assertEquals(1, targetNotes.notes.size)
        assertEquals("new", targetNotes.notes[0].noteText)
    }

    @Test
    fun restoreBackup_ignores_categories_not_requested() = runTest {
        val sourceTags = FakePersonalTagDao().apply { tags.add(PersonalTagEntity(modelId = 2L, tag = "girl", addedAt = 1L)) }
        val backup = buildRepo(tagDao = sourceTags).createBackup(setOf(BackupCategory.TAGS))

        val targetTags = FakePersonalTagDao()
        val targetRepo = buildRepo(tagDao = targetTags)
        // Request only NOTES; TAGS data present in backup must be skipped.
        targetRepo.restoreBackup(backup, RestoreStrategy.MERGE, setOf(BackupCategory.NOTES))
        assertTrue(targetTags.tags.isEmpty())
    }

    // --- Stub DAOs for categories not exercised in these tests ---

    private object StubComfyDao : ComfyUIConnectionDao {
        override fun observeAll(): Flow<List<ComfyUIConnectionEntity>> = MutableStateFlow(emptyList())
        override fun observeActive(): Flow<ComfyUIConnectionEntity?> = MutableStateFlow(null)
        override suspend fun getActive(): ComfyUIConnectionEntity? = null
        override suspend fun getById(id: Long): ComfyUIConnectionEntity? = null
        override suspend fun getAll(): List<ComfyUIConnectionEntity> = emptyList()
        override suspend fun insert(entity: ComfyUIConnectionEntity): Long = 0
        override suspend fun insertAll(entities: List<ComfyUIConnectionEntity>) = Unit
        override suspend fun update(entity: ComfyUIConnectionEntity): Int = 0
        override suspend fun deactivateAll(): Int = 0
        override suspend fun activate(id: Long): Int = 0
        override suspend fun updateTestResult(id: Long, testedAt: Long, success: Boolean): Int = 0
        override suspend fun deleteById(id: Long): Int = 0
        override suspend fun deleteAll(): Int = 0
    }

    private object StubSdDao : SDWebUIConnectionDao {
        override fun observeAll(): Flow<List<SDWebUIConnectionEntity>> = MutableStateFlow(emptyList())
        override fun observeActive(): Flow<SDWebUIConnectionEntity?> = MutableStateFlow(null)
        override suspend fun getActive(): SDWebUIConnectionEntity? = null
        override suspend fun getAll(): List<SDWebUIConnectionEntity> = emptyList()
        override suspend fun insert(entity: SDWebUIConnectionEntity): Long = 0
        override suspend fun insertAll(entities: List<SDWebUIConnectionEntity>) = Unit
        override suspend fun update(entity: SDWebUIConnectionEntity): Int = 0
        override suspend fun deactivateAll(): Int = 0
        override suspend fun activate(id: Long): Int = 0
        override suspend fun updateTestResult(id: Long, testedAt: Long, success: Boolean): Int = 0
        override suspend fun deleteById(id: Long): Int = 0
        override suspend fun deleteAll(): Int = 0
    }

    private object StubExternalDao : ExternalServerConfigDao {
        override fun observeAll(): Flow<List<ExternalServerConfigEntity>> = MutableStateFlow(emptyList())
        override fun observeActive(): Flow<ExternalServerConfigEntity?> = MutableStateFlow(null)
        override suspend fun getActive(): ExternalServerConfigEntity? = null
        override suspend fun getById(id: Long): ExternalServerConfigEntity? = null
        override suspend fun getAll(): List<ExternalServerConfigEntity> = emptyList()
        override suspend fun insert(entity: ExternalServerConfigEntity): Long = 0
        override suspend fun insertAll(entities: List<ExternalServerConfigEntity>) = Unit
        override suspend fun update(entity: ExternalServerConfigEntity): Int = 0
        override suspend fun deactivateAll(): Int = 0
        override suspend fun activate(id: Long): Int = 0
        override suspend fun updateTestResult(id: Long, testedAt: Long, success: Boolean): Int = 0
        override suspend fun deleteById(id: Long): Int = 0
        override suspend fun deleteAll(): Int = 0
    }

    private object StubFollowDao : FollowedCreatorDao {
        override suspend fun insert(entity: FollowedCreatorEntity) = Unit
        override suspend fun insertAll(entities: List<FollowedCreatorEntity>) = Unit
        override suspend fun deleteAll(): Int = 0
        override suspend fun delete(username: String): Int = 0
        override fun isFollowing(username: String): Flow<Boolean> = MutableStateFlow(false)
        override fun observeAll(): Flow<List<FollowedCreatorEntity>> = MutableStateFlow(emptyList())
        override suspend fun getAll(): List<FollowedCreatorEntity> = emptyList()
        override suspend fun updateLastCheckedAt(username: String, timestamp: Long): Int = 0
    }

    private object StubHiddenDao : HiddenModelDao {
        override suspend fun getAllIds(): List<Long> = emptyList()
        override suspend fun getAll(): List<HiddenModelEntity> = emptyList()
        override suspend fun insert(entity: HiddenModelEntity) = Unit
        override suspend fun insertAll(entities: List<HiddenModelEntity>) = Unit
        override suspend fun deleteAll(): Int = 0
        override suspend fun delete(modelId: Long): Int = 0
    }

    private object StubExcludedDao : ExcludedTagDao {
        override suspend fun getAll(): List<ExcludedTagEntity> = emptyList()
        override suspend fun insert(entity: ExcludedTagEntity) = Unit
        override suspend fun insertAll(entities: List<ExcludedTagEntity>) = Unit
        override suspend fun deleteAll(): Int = 0
        override suspend fun delete(tag: String): Int = 0
    }

    private object StubPromptDao : SavedPromptDao {
        override fun observeAll(): Flow<List<SavedPromptEntity>> = MutableStateFlow(emptyList())
        override fun observeTemplates(): Flow<List<SavedPromptEntity>> = MutableStateFlow(emptyList())
        override fun observeHistory(): Flow<List<SavedPromptEntity>> = MutableStateFlow(emptyList())
        override fun search(query: String): Flow<List<SavedPromptEntity>> = MutableStateFlow(emptyList())
        override suspend fun countByPromptAndModel(prompt: String, modelName: String?): Int = 0
        override suspend fun updateTemplate(id: Long, isTemplate: Boolean, templateName: String?): Int = 0
        override suspend fun getAllUserCreated(): List<SavedPromptEntity> = emptyList()
        override suspend fun insert(entity: SavedPromptEntity) = Unit
        override suspend fun insertAll(entities: List<SavedPromptEntity>) = Unit
        override suspend fun upsert(entity: SavedPromptEntity) = Unit
        override suspend fun deleteAllUserCreated(): Int = 0
        override suspend fun deleteById(id: Long): Int = 0
    }

    private object StubFilterDao : SavedSearchFilterDao {
        override fun observeAll(): Flow<List<SavedSearchFilterEntity>> = MutableStateFlow(emptyList())
        override suspend fun getAll(): List<SavedSearchFilterEntity> = emptyList()
        override suspend fun insert(entity: SavedSearchFilterEntity): Long = 0
        override suspend fun insertAll(entities: List<SavedSearchFilterEntity>) = Unit
        override suspend fun deleteAll(): Int = 0
        override suspend fun deleteById(id: Long): Int = 0
    }

    private object StubPrefsDao : UserPreferencesDao {
        override fun observePreferences(): Flow<UserPreferencesEntity?> = MutableStateFlow(null)
        override suspend fun getPreferences(): UserPreferencesEntity? = null
        override suspend fun upsert(entity: UserPreferencesEntity) = Unit
    }
}
