package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import com.riox432.civitdeck.testSavedPrompt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SavedPromptUseCasesTest {

    private val prompts = listOf(
        testSavedPrompt(id = 1L, prompt = "prompt1"),
        testSavedPrompt(id = 2L, prompt = "prompt2"),
    )

    private class FakeSavedPromptRepository(
        private val prompts: List<SavedPrompt>,
    ) : SavedPromptRepository {
        var saveCalled = false
        var lastSavedMeta: ImageGenerationMeta? = null
        var lastSavedUrl: String? = null
        var deleteCalled = false
        var lastDeletedId: Long? = null

        override fun observeAll(): Flow<List<SavedPrompt>> = flowOf(prompts)
        override suspend fun save(meta: ImageGenerationMeta, sourceImageUrl: String?) {
            saveCalled = true
            lastSavedMeta = meta
            lastSavedUrl = sourceImageUrl
        }
        override suspend fun delete(id: Long) {
            deleteCalled = true
            lastDeletedId = id
        }
    }

    private val repo = FakeSavedPromptRepository(prompts)

    @Test
    fun observeSavedPrompts_emits_list() = runTest {
        val useCase = ObserveSavedPromptsUseCase(repo)
        val result = useCase().first()
        assertEquals(2, result.size)
        assertEquals("prompt1", result[0].prompt)
    }

    @Test
    fun savePrompt_delegates() = runTest {
        val meta = ImageGenerationMeta(
            prompt = "1girl",
            negativePrompt = "bad",
            sampler = "Euler",
            cfgScale = 7.0,
            steps = 20,
            seed = 123L,
            model = "Model",
            size = "512x512",
        )
        val useCase = SavePromptUseCase(repo)
        useCase(meta, "https://example.com/img.jpg")
        assertTrue(repo.saveCalled)
        assertEquals("1girl", repo.lastSavedMeta?.prompt)
        assertEquals("https://example.com/img.jpg", repo.lastSavedUrl)
    }

    @Test
    fun deletePrompt_delegates() = runTest {
        val useCase = DeleteSavedPromptUseCase(repo)
        useCase(42L)
        assertTrue(repo.deleteCalled)
        assertEquals(42L, repo.lastDeletedId)
    }
}
