package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.dao.ModelNoteDao
import com.riox432.civitdeck.data.local.dao.PersonalTagDao
import com.riox432.civitdeck.data.local.entity.ModelNoteEntity
import com.riox432.civitdeck.data.local.entity.PersonalTagEntity
import com.riox432.civitdeck.domain.model.ModelNote
import com.riox432.civitdeck.domain.model.PersonalTag
import com.riox432.civitdeck.domain.repository.ModelNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.riox432.civitdeck.data.local.currentTimeMillis

class ModelNoteRepositoryImpl(
    private val noteDao: ModelNoteDao,
    private val tagDao: PersonalTagDao,
) : ModelNoteRepository {

    override fun observeNoteForModel(modelId: Long): Flow<ModelNote?> =
        noteDao.observeByModelId(modelId).map { it?.toDomain() }

    override suspend fun saveNote(modelId: Long, noteText: String) {
        val now = currentTimeMillis()
        val existing = noteDao.getByModelId(modelId)
        val entity = if (existing != null) {
            existing.copy(noteText = noteText, updatedAt = now)
        } else {
            ModelNoteEntity(
                modelId = modelId,
                noteText = noteText,
                createdAt = now,
                updatedAt = now,
            )
        }
        noteDao.upsert(entity)
    }

    override suspend fun deleteNote(modelId: Long) {
        noteDao.deleteByModelId(modelId)
    }

    override fun observeTagsForModel(modelId: Long): Flow<List<PersonalTag>> =
        tagDao.observeByModelId(modelId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun addTag(modelId: Long, tag: String) {
        val now = currentTimeMillis()
        tagDao.insert(PersonalTagEntity(modelId = modelId, tag = tag, addedAt = now))
    }

    override suspend fun removeTag(modelId: Long, tag: String) {
        tagDao.delete(modelId, tag)
    }

    override suspend fun getAllTags(): List<String> = tagDao.getAllDistinctTags()

    override suspend fun getModelIdsByTag(tag: String): List<Long> =
        tagDao.getModelIdsByTag(tag)

    private fun ModelNoteEntity.toDomain() = ModelNote(
        id = id,
        modelId = modelId,
        noteText = noteText,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun PersonalTagEntity.toDomain() = PersonalTag(
        id = id,
        modelId = modelId,
        tag = tag,
        addedAt = addedAt,
    )
}
