package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.repository.ModelFileHashRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the logic-bearing CivitAI<->ComfyUI bridge use cases in ComfyUIUseCases.kt:
 * workflow JSON validation, case-insensitive local hash matching, and metadata->params mapping.
 */
class ComfyUIBridgeUseCasesTest {

    // region ImportWorkflowUseCase

    @Test
    fun importWorkflow_returnsTrimmedJson_forValidObject() {
        val json = """  {"3": {"class_type": "KSampler"}}  """
        val result = ImportWorkflowUseCase()(json)
        assertEquals("""{"3": {"class_type": "KSampler"}}""", result)
    }

    @Test
    fun importWorkflow_throwsForBlankInput() {
        val error = assertFailsWith<IllegalStateException> { ImportWorkflowUseCase()("   ") }
        assertTrue(error.message!!.contains("empty"))
    }

    @Test
    fun importWorkflow_throwsForInvalidJson() {
        val error = assertFailsWith<IllegalStateException> { ImportWorkflowUseCase()("not json") }
        assertTrue(error.message!!.contains("Invalid JSON"))
    }

    @Test
    fun importWorkflow_throwsWhenNotAJsonObject() {
        val error = assertFailsWith<IllegalStateException> { ImportWorkflowUseCase()("[1, 2, 3]") }
        assertTrue(error.message!!.contains("must be a JSON object"))
    }

    @Test
    fun importWorkflow_throwsForEmptyObject() {
        val error = assertFailsWith<IllegalStateException> { ImportWorkflowUseCase()("{}") }
        assertTrue(error.message!!.contains("no nodes"))
    }

    // endregion

    // region FindMatchingLocalModelUseCase

    @Test
    fun findMatchingLocalModel_matchesHashCaseInsensitively() = runTest {
        val repo = FakeHashRepo(setOf("abc123def"))
        val useCase = FindMatchingLocalModelUseCase(repo)

        // Query hash differs only by case — must still match.
        val result = useCase("ABC123DEF")

        assertEquals("ABC123DEF", result)
    }

    @Test
    fun findMatchingLocalModel_returnsNullWhenNoMatch() = runTest {
        val repo = FakeHashRepo(setOf("abc123"))
        val useCase = FindMatchingLocalModelUseCase(repo)

        assertNull(useCase("ffffff"))
    }

    // endregion

    // region PopulateGenerationFromModelUseCase

    @Test
    fun populateGeneration_appliesDefaultsForNullMetadata() {
        val params = PopulateGenerationFromModelUseCase()(
            prompt = null,
            negativePrompt = null,
            steps = null,
            cfgScale = null,
            seed = null,
            sampler = null,
            checkpointName = "model.safetensors",
        )

        assertEquals("model.safetensors", params.checkpoint)
        assertEquals("", params.prompt)
        assertEquals(ComfyUIGenerationParams.DEFAULT_STEPS, params.steps)
        assertEquals(ComfyUIGenerationParams.DEFAULT_CFG, params.cfgScale)
        assertEquals(-1L, params.seed)
        assertEquals(ComfyUIGenerationParams.DEFAULT_SAMPLER, params.samplerName)
    }

    @Test
    fun populateGeneration_normalizesSamplerName() {
        val params = PopulateGenerationFromModelUseCase()(
            prompt = "a cat",
            negativePrompt = "blurry",
            steps = 30,
            cfgScale = 8.0,
            seed = 42L,
            sampler = "DPM++ 2M Karras",
            checkpointName = "ckpt",
        )

        // Lowercased, spaces and dashes converted to underscores.
        assertEquals("dpm++_2m_karras", params.samplerName)
        assertEquals("a cat", params.prompt)
        assertEquals(30, params.steps)
        assertEquals(42L, params.seed)
    }

    // endregion

    private class FakeHashRepo(private val owned: Set<String>) : ModelFileHashRepository {
        override suspend fun getOwnedHashes(): Set<String> = owned
        override suspend fun verifyFileHash(fileId: Long, sha256Hash: String) = Unit
        override fun observeOwnedHashes(): Flow<Set<String>> = flowOf(owned)
        override fun observeFileCount(): Flow<Int> = flowOf(0)
        override fun observeMatchedCount(): Flow<Int> = flowOf(0)
        override fun observeUpdatesAvailableCount(): Flow<Int> = flowOf(0)
    }
}
