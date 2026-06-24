package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.domain.model.TemplateVariable
import com.riox432.civitdeck.domain.model.TemplateVariableType
import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.domain.model.WorkflowTemplateCategory
import com.riox432.civitdeck.domain.model.WorkflowTemplateType
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies [ExportWorkflowTemplateUseCase] serializes a [WorkflowTemplate] into export DTO JSON
 * and that the output round-trips back through [ImportWorkflowTemplateUseCase] (legacy path).
 */
class ExportWorkflowTemplateUseCaseTest {

    private val export = ExportWorkflowTemplateUseCase()

    @Test
    fun serializesTemplateFieldsAndVariables() {
        val template = template(
            name = "My Upscaler",
            type = WorkflowTemplateType.UPSCALE,
            category = WorkflowTemplateCategory.ANIME,
            variables = listOf(
                TemplateVariable(name = "scale", type = TemplateVariableType.NUMBER, defaultValue = "2"),
            ),
        )

        val json = export(template)

        assertTrue(json.contains("\"name\": \"My Upscaler\""))
        assertTrue(json.contains("\"type\": \"UPSCALE\""))
        assertTrue(json.contains("\"category\": \"ANIME\""))
        assertTrue(json.contains("\"scale\""))
    }

    @Test
    fun exportedJsonImportsBackToEquivalentTemplate() = runTest {
        val original = template(
            name = "Round Trip",
            type = WorkflowTemplateType.TXT2IMG,
            category = WorkflowTemplateCategory.GENERAL,
            variables = listOf(
                TemplateVariable(name = "a", type = TemplateVariableType.TEXT, defaultValue = "x"),
            ),
        )
        val json = export(original)
        val repo = RecordingPromptRepo()
        val import = ImportWorkflowTemplateUseCase(
            repo,
            ParseAppModeMetadataUseCase(),
            ExtractWorkflowParametersUseCase(ParseAppModeMetadataUseCase()),
        )

        import(json)

        val saved = repo.saved.single()
        assertEquals("Round Trip", saved.templateName)
        assertEquals("TXT2IMG", saved.templateType)
    }

    private fun template(
        name: String,
        type: WorkflowTemplateType,
        category: WorkflowTemplateCategory,
        variables: List<TemplateVariable>,
    ) = WorkflowTemplate(
        id = 1L,
        name = name,
        description = "desc",
        type = type,
        category = category,
        variables = variables,
        isBuiltIn = false,
        version = 1,
        author = "me",
        createdAt = 0L,
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
