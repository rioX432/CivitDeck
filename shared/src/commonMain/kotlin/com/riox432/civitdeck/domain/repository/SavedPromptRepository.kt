package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.SavedPrompt
import kotlinx.coroutines.flow.Flow

interface SavedPromptRepository {
    fun observeAll(): Flow<List<SavedPrompt>>
    fun observeTemplates(): Flow<List<SavedPrompt>>
    fun observeHistory(): Flow<List<SavedPrompt>>
    fun search(query: String): Flow<List<SavedPrompt>>
    suspend fun save(meta: ImageGenerationMeta, sourceImageUrl: String?)
    suspend fun autoSave(meta: ImageGenerationMeta, sourceImageUrl: String?)
    suspend fun toggleTemplate(id: Long, isTemplate: Boolean, templateName: String?)
    suspend fun updateCategory(id: Long, category: String?)
    suspend fun delete(id: Long)
}
