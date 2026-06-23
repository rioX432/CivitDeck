package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.dao.ModelNoteDao
import com.riox432.civitdeck.data.local.dao.PersonalTagDao
import com.riox432.civitdeck.data.local.entity.ModelNoteEntity
import com.riox432.civitdeck.data.local.entity.PersonalTagEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [ModelNoteRepositoryImpl] covering note upsert (insert vs update),
 * deletion, tag add/remove, and distinct-tag/model-id lookups via fake DAOs.
 */
class ModelNoteRepositoryImplTest {

    private class FakeNoteDao : ModelNoteDao {
        val notes = mutableListOf<ModelNoteEntity>()
        private var idCounter = 1L
        private val flow = MutableStateFlow<ModelNoteEntity?>(null)

        override fun observeByModelId(modelId: Long): Flow<ModelNoteEntity?> = flow

        override suspend fun getByModelId(modelId: Long): ModelNoteEntity? =
            notes.firstOrNull { it.modelId == modelId }

        override suspend fun getAll(): List<ModelNoteEntity> = notes.toList()

        override suspend fun upsert(entity: ModelNoteEntity) {
            val idx = notes.indexOfFirst { it.modelId == entity.modelId }
            val stored = if (entity.id == 0L) entity.copy(id = idCounter++) else entity
            if (idx >= 0) notes[idx] = stored else notes.add(stored)
            flow.value = notes.firstOrNull { it.modelId == entity.modelId }
        }

        override suspend fun insertAll(entities: List<ModelNoteEntity>) {
            entities.forEach { upsert(it) }
        }

        override suspend fun deleteAll(): Int {
            val count = notes.size
            notes.clear()
            flow.value = null
            return count
        }

        override suspend fun deleteByModelId(modelId: Long): Int {
            val before = notes.size
            notes.removeAll { it.modelId == modelId }
            flow.value = null
            return before - notes.size
        }
    }

    private class FakeTagDao : PersonalTagDao {
        val tags = mutableListOf<PersonalTagEntity>()
        private var idCounter = 1L

        override fun observeByModelId(modelId: Long): Flow<List<PersonalTagEntity>> =
            MutableStateFlow(tags.filter { it.modelId == modelId })

        override suspend fun getAll(): List<PersonalTagEntity> = tags.toList()

        override suspend fun insert(entity: PersonalTagEntity) {
            val exists = tags.any { it.modelId == entity.modelId && it.tag == entity.tag }
            if (!exists) tags.add(entity.copy(id = idCounter++))
        }

        override suspend fun insertAll(entities: List<PersonalTagEntity>) {
            entities.forEach { insert(it) }
        }

        override suspend fun delete(modelId: Long, tag: String): Int {
            val before = tags.size
            tags.removeAll { it.modelId == modelId && it.tag == tag }
            return before - tags.size
        }

        override suspend fun deleteAll(): Int {
            val count = tags.size
            tags.clear()
            return count
        }

        override suspend fun getAllDistinctTags(): List<String> =
            tags.map { it.tag }.distinct().sorted()

        override suspend fun getModelIdsByTag(tag: String): List<Long> =
            tags.filter { it.tag == tag }.map { it.modelId }.distinct()
    }

    @Test
    fun saveNote_inserts_new_note() = runTest {
        val noteDao = FakeNoteDao()
        val repo = ModelNoteRepositoryImpl(noteDao, FakeTagDao())
        repo.saveNote(1L, "hello")
        assertEquals(1, noteDao.notes.size)
        assertEquals("hello", noteDao.notes[0].noteText)
    }

    @Test
    fun saveNote_updates_existing_note_preserving_id() = runTest {
        val noteDao = FakeNoteDao()
        val repo = ModelNoteRepositoryImpl(noteDao, FakeTagDao())
        repo.saveNote(1L, "first")
        val firstId = noteDao.notes[0].id
        repo.saveNote(1L, "second")
        assertEquals(1, noteDao.notes.size)
        assertEquals("second", noteDao.notes[0].noteText)
        assertEquals(firstId, noteDao.notes[0].id)
    }

    @Test
    fun observeNoteForModel_maps_entity_to_domain() = runTest {
        val noteDao = FakeNoteDao()
        val repo = ModelNoteRepositoryImpl(noteDao, FakeTagDao())
        repo.saveNote(1L, "note text")
        val note = repo.observeNoteForModel(1L).first()
        assertEquals("note text", note?.noteText)
        assertEquals(1L, note?.modelId)
    }

    @Test
    fun deleteNote_removes_note() = runTest {
        val noteDao = FakeNoteDao()
        val repo = ModelNoteRepositoryImpl(noteDao, FakeTagDao())
        repo.saveNote(1L, "x")
        repo.deleteNote(1L)
        assertTrue(noteDao.notes.isEmpty())
    }

    @Test
    fun addTag_is_idempotent_for_duplicates() = runTest {
        val tagDao = FakeTagDao()
        val repo = ModelNoteRepositoryImpl(FakeNoteDao(), tagDao)
        repo.addTag(1L, "anime")
        repo.addTag(1L, "anime")
        assertEquals(1, tagDao.tags.size)
    }

    @Test
    fun removeTag_deletes_matching_tag() = runTest {
        val tagDao = FakeTagDao()
        val repo = ModelNoteRepositoryImpl(FakeNoteDao(), tagDao)
        repo.addTag(1L, "anime")
        repo.addTag(1L, "girl")
        repo.removeTag(1L, "anime")
        assertEquals(listOf("girl"), tagDao.tags.map { it.tag })
    }

    @Test
    fun getAllTags_returns_distinct_sorted_tags() = runTest {
        val tagDao = FakeTagDao()
        val repo = ModelNoteRepositoryImpl(FakeNoteDao(), tagDao)
        repo.addTag(1L, "zebra")
        repo.addTag(2L, "zebra")
        repo.addTag(1L, "anime")
        assertEquals(listOf("anime", "zebra"), repo.getAllTags())
    }

    @Test
    fun getModelIdsByTag_returns_distinct_model_ids() = runTest {
        val tagDao = FakeTagDao()
        val repo = ModelNoteRepositoryImpl(FakeNoteDao(), tagDao)
        repo.addTag(1L, "anime")
        repo.addTag(2L, "anime")
        repo.addTag(3L, "other")
        assertEquals(setOf(1L, 2L), repo.getModelIdsByTag("anime").toSet())
    }

    @Test
    fun observeNoteForModel_returns_null_when_absent() = runTest {
        val repo = ModelNoteRepositoryImpl(FakeNoteDao(), FakeTagDao())
        assertNull(repo.observeNoteForModel(1L).first())
    }
}
