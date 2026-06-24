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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers all three import paths of [ImportWorkflowTemplateUseCase]:
 * - Path 1: raw ComfyUI workflow with APP mode metadata -> auto template from designated inputs
 * - Path 2: raw ComfyUI workflow without APP mode -> auto template from PRIORITY_NODES
 * - Path 3: legacy ImportDto -> explicit template with type/category resolution
 */
class ImportWorkflowTemplateUseCaseTest {

    private fun useCase(repo: SavedPromptRepository): ImportWorkflowTemplateUseCase {
        val parseAppMode = ParseAppModeMetadataUseCase()
        val extract = ExtractWorkflowParametersUseCase(parseAppMode)
        return ImportWorkflowTemplateUseCase(repo, parseAppMode, extract)
    }

    // region Path 1: APP mode extraction

    @Test
    fun imports_app_mode_workflow_as_auto_template_with_app_mode_flag() = runTest {
        val repo = RecordingPromptRepo()
        val json = """
            {
              "3": {
                "class_type": "KSampler",
                "inputs": {"seed": 42, "steps": 20, "cfg": 7.0},
                "_meta": {"title": "KSampler"}
              },
              "6": {
                "class_type": "CLIPTextEncode",
                "inputs": {"text": "a cat"},
                "_meta": {"title": "Prompt"}
              },
              "extra": {
                "linearData": {"inputs": [["3", "seed"], ["6", "text"]], "outputs": []},
                "linearMode": true
              }
            }
        """.trimIndent()

        useCase(repo).invoke(json)

        val saved = repo.saved.single()
        assertTrue(saved.isAppMode)
        assertTrue(saved.isTemplate)
        // The raw workflow JSON is preserved for later re-injection.
        assertEquals(json, saved.rawWorkflowJson)
        // Two designated inputs become two template variables.
        assertTrue(saved.templateMetadata!!.contains("Auto-detected from APP mode"))
    }

    @Test
    fun app_mode_template_name_resolves_from_extra_title() = runTest {
        val repo = RecordingPromptRepo()
        val json = """
            {
              "6": {
                "class_type": "CLIPTextEncode",
                "inputs": {"text": "a cat"},
                "_meta": {"title": "Prompt"}
              },
              "extra": {
                "title": "My Portrait Workflow",
                "linearData": {"inputs": [["6", "text"]], "outputs": []},
                "linearMode": true
              }
            }
        """.trimIndent()

        useCase(repo).invoke(json)

        assertEquals("My Portrait Workflow", repo.saved.single().templateName)
    }

    @Test
    fun app_mode_with_no_extractable_inputs_falls_through_to_legacy_dto() = runTest {
        val repo = RecordingPromptRepo()
        // APP mode metadata is present, but the designated input references a node
        // that does not exist, so extraction yields no params and path 1 is skipped.
        // The workflow has no priority nodes either, so path 2 also yields nothing,
        // and the DTO parse on path 3 fails because this is not a valid ImportDto.
        val json = """
            {
              "extra": {
                "linearData": {"inputs": [["999", "text"]], "outputs": []},
                "linearMode": true
              }
            }
        """.trimIndent()

        assertFailsWith<IllegalStateException> { useCase(repo).invoke(json) }
        assertTrue(repo.saved.isEmpty())
    }

    // endregion

    // region Path 2: raw workflow extraction (PRIORITY_NODES, no APP mode)

    @Test
    fun imports_raw_workflow_as_auto_template_without_app_mode() = runTest {
        val repo = RecordingPromptRepo()
        val json = """
            {
              "3": {
                "class_type": "KSampler",
                "inputs": {"seed": 42, "steps": 20, "cfg": 7.0},
                "_meta": {"title": "KSampler"}
              }
            }
        """.trimIndent()

        useCase(repo).invoke(json)

        val saved = repo.saved.single()
        assertFalse(saved.isAppMode)
        assertTrue(saved.isTemplate)
        assertEquals(json, saved.rawWorkflowJson)
        assertTrue(saved.templateMetadata!!.contains("Auto-extracted from workflow"))
        // Default name when no extra.title is present.
        assertEquals("Imported Workflow", saved.templateName)
    }

    @Test
    fun raw_workflow_infers_lora_type_from_node_class() = runTest {
        val repo = RecordingPromptRepo()
        val json = """
            {
              "5": {
                "class_type": "LoraLoader",
                "inputs": {"lora_name": "anime.safetensors", "strength_model": 0.8},
                "_meta": {"title": "Lora"}
              }
            }
        """.trimIndent()

        useCase(repo).invoke(json)

        assertEquals("LORA", repo.saved.single().templateType)
    }

    @Test
    fun raw_workflow_without_priority_nodes_falls_through_to_legacy_dto() = runTest {
        val repo = RecordingPromptRepo()
        // Has class_type nodes (looks like a raw workflow) but none are PRIORITY_NODES,
        // so extraction is empty and path 2 is skipped; path 3 DTO parse then fails.
        val json = """
            {
              "10": {
                "class_type": "CustomNode",
                "inputs": {"my_param": "value"},
                "_meta": {"title": "Custom"}
              }
            }
        """.trimIndent()

        assertFailsWith<IllegalStateException> { useCase(repo).invoke(json) }
        assertTrue(repo.saved.isEmpty())
    }

    // endregion

    // region Path 3: legacy ImportDto

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

    // endregion

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
