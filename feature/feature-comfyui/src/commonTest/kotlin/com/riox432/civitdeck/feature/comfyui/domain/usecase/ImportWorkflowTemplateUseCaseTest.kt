package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Covers the legacy ImportDto path of [ImportWorkflowTemplateUseCase] (path 3):
 * JSON that is neither APP mode nor a raw node workflow is decoded as an explicit
 * template definition, with type/category resolution and failure handling.
 */
class ImportWorkflowTemplateUseCaseTest {

    private fun useCase(repo: SavedPromptRepository): ImportWorkflowTemplateUseCase {
        val parseAppMode = ParseAppModeMetadataUseCase()
        val extract = ExtractWorkflowParametersUseCase(parseAppMode)
        return ImportWorkflowTemplateUseCase(repo, parseAppMode, extract)
    }

    @Test
    fun imports_legacy_dto_and_saves_template_with_resolved_type_and_category() = runTest {
        val repo = RecordingPromptRepo()
        val json = """
            {
              "name": "My Upscaler",
              "description": "desc",
              "type": "UPSCALE",
              "category": "ANIME",
              "version": 3,
              "author": "me",
              "variables": [
                {"name": "scale", "type": "NUMBER", "defaultValue": "2"}
              ]
            }
        """.trimIndent()

        useCase(repo).invoke(json)

        val saved = repo.saved.single()
        assertEquals("My Upscaler", saved.templateName)
        assertEquals("UPSCALE", saved.templateType)
        assertTrue(saved.isTemplate)
        // Category is embedded in the metadata JSON blob.
        assertTrue(saved.templateMetadata!!.contains("ANIME"))
    }

    @Test
    fun falls_back_to_general_category_when_category_is_unknown() = runTest {
        val repo = RecordingPromptRepo()
        val json = """
            {"name": "X", "type": "TXT2IMG", "category": "NOPE",
             "variables": [{"name": "a", "type": "TEXT", "defaultValue": ""}]}
        """.trimIndent()

        useCase(repo).invoke(json)

        assertTrue(repo.saved.single().templateMetadata!!.contains("GENERAL"))
    }

    @Test
    fun throws_when_template_type_is_unknown() = runTest {
        val repo = RecordingPromptRepo()
        val json = """
            {"name": "X", "type": "BOGUS",
             "variables": [{"name": "a", "type": "TEXT", "defaultValue": ""}]}
        """.trimIndent()

        val error = assertFailsWith<IllegalStateException> { useCase(repo).invoke(json) }
        assertTrue(error.message!!.contains("Unknown template type"))
        assertTrue(repo.saved.isEmpty())
    }

    @Test
    fun throws_when_json_is_blank() = runTest {
        val repo = RecordingPromptRepo()

        assertFailsWith<IllegalStateException> { useCase(repo).invoke("   ") }
        assertTrue(repo.saved.isEmpty())
    }

    @Test
    fun throws_when_dto_is_structurally_invalid() = runTest {
        val repo = RecordingPromptRepo()
        // Not a node workflow and missing required DTO fields (name/type/variables).
        val json = """{"unrelated": 1}"""

        assertFailsWith<IllegalStateException> { useCase(repo).invoke(json) }
        assertTrue(repo.saved.isEmpty())
    }

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
        override suspend fun toggleTemplate(id: Long, isTemplate: Boolean, templateName: String?) =
            Unit
        override suspend fun delete(id: Long) = Unit
    }
}
