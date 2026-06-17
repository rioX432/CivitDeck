package com.riox432.civitdeck.testing

import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [SavedPromptRepository] for ViewModel tests.
 *
 * Records the last save/auto-save/delete so tests can assert that a ViewModel
 * forwarded an action, while exposing mutable flows for observed lists.
 */
class FakeSavedPromptRepository(
    prompts: List<SavedPrompt> = emptyList(),
    templates: List<SavedPrompt> = emptyList(),
    history: List<SavedPrompt> = emptyList(),
) : SavedPromptRepository {

    val promptsFlow = MutableStateFlow(prompts)
    val templatesFlow = MutableStateFlow(templates)
    val historyFlow = MutableStateFlow(history)
    val searchResultsFlow = MutableStateFlow(emptyList<SavedPrompt>())

    var savedMeta: ImageGenerationMeta? = null
    var deletedId: Long? = null

    override fun observeAll(): Flow<List<SavedPrompt>> = promptsFlow
    override fun observeTemplates(): Flow<List<SavedPrompt>> = templatesFlow
    override fun observeHistory(): Flow<List<SavedPrompt>> = historyFlow
    override fun search(query: String): Flow<List<SavedPrompt>> = searchResultsFlow

    override suspend fun save(meta: ImageGenerationMeta, sourceImageUrl: String?) {
        savedMeta = meta
    }

    override suspend fun autoSave(meta: ImageGenerationMeta, sourceImageUrl: String?) {
        savedMeta = meta
    }

    override suspend fun toggleTemplate(id: Long, isTemplate: Boolean, templateName: String?) = Unit

    override suspend fun delete(id: Long) {
        deletedId = id
    }

    override suspend fun saveTemplate(prompt: SavedPrompt) = Unit
}
