package com.riox432.civitdeck.feature.comfyui.domain.usecase

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
 * Verifies [SaveWorkflowTemplateUseCase] injects a creation timestamp only when one is absent
 * and persists the template via the repository.
 */
class SaveWorkflowTemplateUseCaseTest {

    @Test
    fun injectsCreatedAtWhenZero() = runTest {
        val repo = RecordingPromptRepo()
        val useCase = SaveWorkflowTemplateUseCase(repo)

        useCase(template(name = "Fresh", createdAt = 0L))

        // savedAt carries the injected timestamp; it must be non-zero now.
        assertTrue(repo.saved.single().savedAt > 0L)
    }

    @Test
    fun preservesExistingCreatedAt() = runTest {
        val repo = RecordingPromptRepo()
        val useCase = SaveWorkflowTemplateUseCase(repo)

        useCase(template(name = "Existing", createdAt = 42L))

        assertEquals(42L, repo.saved.single().savedAt)
    }

    private fun template(name: String, createdAt: Long) = WorkflowTemplate(
        id = 1L,
        name = name,
        type = WorkflowTemplateType.TXT2IMG,
        variables = emptyList(),
        isBuiltIn = false,
        createdAt = createdAt,
    )

    private class RecordingPromptRepo : SavedPromptRepository {
        val saved = mutableListOf<SavedPrompt>()
        override suspend fun saveTemplate(prompt: SavedPrompt) {
            saved.add(prompt)
        }
        override fun observeAll(): Flow<List<SavedPrompt>> = flowOf(emptyList())
        override fun observeTemplates(): Flow<List<SavedPrompt>> = flowOf(emptyList())
        override fun observeHistory(): Flow<List<SavedPrompt>> = flowOf(emptyList())
        override fun search(query: String): Flow<List<SavedPrompt>> = flowOf(emptyList())
        override suspend fun save(meta: ImageGenerationMeta, sourceImageUrl: String?) = Unit
        override suspend fun autoSave(meta: ImageGenerationMeta, sourceImageUrl: String?) = Unit
        override suspend fun toggleTemplate(id: Long, isTemplate: Boolean, templateName: String?) = Unit
        override suspend fun delete(id: Long) = Unit
    }
}
