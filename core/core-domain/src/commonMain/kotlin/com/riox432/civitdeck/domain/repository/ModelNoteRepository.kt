package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ModelNote
import com.riox432.civitdeck.domain.model.PersonalTag
import kotlinx.coroutines.flow.Flow

interface ModelNoteRepository {
    // Notes
    fun observeNoteForModel(modelId: Long): Flow<ModelNote?>
    suspend fun saveNote(modelId: Long, noteText: String)
    suspend fun deleteNote(modelId: Long)

    // Personal tags
    fun observeTagsForModel(modelId: Long): Flow<List<PersonalTag>>
    suspend fun addTag(modelId: Long, tag: String)
    suspend fun removeTag(modelId: Long, tag: String)
    suspend fun getAllTags(): List<String>
    suspend fun getModelIdsByTag(tag: String): List<Long>
}
