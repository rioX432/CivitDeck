package com.riox432.civitdeck.feature.comfyui.domain.usecase

import app.cash.turbine.test
import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.domain.model.WorkflowTemplateType
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies [GetWorkflowTemplatesUseCase] maps stored prompts to templates (dropping unparseable
 * rows) and sorts built-in templates first, then newest-created first.
 */
class GetWorkflowTemplatesUseCaseTest {

    @Test
    fun sortsBuiltInFirstThenNewestCreated() = runTest {
        // id < 0 => built-in (per SavedPrompt.toWorkflowTemplate mapping).
        val builtIn = templatePrompt(id = -1L, name = "BuiltIn", createdAt = 100L)
        val newUser = templatePrompt(id = 1L, name = "NewUser", createdAt = 300L)
        val oldUser = templatePrompt(id = 2L, name = "OldUser", createdAt = 200L)
        val useCase = GetWorkflowTemplatesUseCase(FakeRepo(listOf(oldUser, newUser, builtIn)))

        useCase().test {
            val names = awaitItem().map { it.name }
            // Built-in first, then user templates by createdAt descending.
            assertEquals(listOf("BuiltIn", "NewUser", "OldUser"), names)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dropsRowsThatDoNotParseAsTemplates() = runTest {
        val valid = templatePrompt(id = 1L, name = "Valid", createdAt = 1L)
        // templateType null => toWorkflowTemplate returns null and the row is dropped.
        val invalid = valid.copy(id = 2L, templateName = "Broken", templateType = null)
        val useCase = GetWorkflowTemplatesUseCase(FakeRepo(listOf(valid, invalid)))

        useCase().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Valid", result.single().name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emitsEmptyListWhenNoTemplates() = runTest {
        val useCase = GetWorkflowTemplatesUseCase(FakeRepo(emptyList()))

        useCase().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun templatePrompt(id: Long, name: String, createdAt: Long): SavedPrompt =
        WorkflowTemplate(
            id = id,
            name = name,
            type = WorkflowTemplateType.TXT2IMG,
            variables = emptyList(),
            isBuiltIn = id < 0L,
            createdAt = createdAt,
        ).toSavedPrompt().copy(id = id, savedAt = createdAt)

    private class FakeRepo(private val templates: List<SavedPrompt>) : SavedPromptRepository {
        override fun observeTemplates(): Flow<List<SavedPrompt>> = flowOf(templates)
        override suspend fun saveTemplate(prompt: SavedPrompt) = Unit
        override fun observeAll(): Flow<List<SavedPrompt>> = flowOf(emptyList())
        override fun observeHistory(): Flow<List<SavedPrompt>> = flowOf(emptyList())
        override fun search(query: String): Flow<List<SavedPrompt>> = flowOf(emptyList())
        override suspend fun save(meta: ImageGenerationMeta, sourceImageUrl: String?) = Unit
        override suspend fun autoSave(meta: ImageGenerationMeta, sourceImageUrl: String?) = Unit
        override suspend fun toggleTemplate(id: Long, isTemplate: Boolean, templateName: String?) = Unit
        override suspend fun delete(id: Long) = Unit
    }
}
