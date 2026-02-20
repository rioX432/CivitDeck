package com.riox432.civitdeck.ui.prompts

import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import com.riox432.civitdeck.domain.usecase.DeleteSavedPromptUseCase
import com.riox432.civitdeck.domain.usecase.ObserveSavedPromptsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SavedPromptsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val testPrompts = listOf(
        SavedPrompt(
            id = 1L, prompt = "1girl", negativePrompt = null, sampler = null,
            steps = null, cfgScale = null, seed = null, modelName = null,
            size = null, sourceImageUrl = null, savedAt = 1000L,
        ),
    )

    private class FakeRepo(
        private val prompts: List<SavedPrompt>,
    ) : SavedPromptRepository {
        var deletedId: Long? = null

        override fun observeAll(): Flow<List<SavedPrompt>> = flowOf(prompts)
        override suspend fun save(meta: ImageGenerationMeta, sourceImageUrl: String?) =
            error("not used")
        override suspend fun delete(id: Long) { deletedId = id }
    }

    @Test
    fun prompts_flow_emits_list() = runTest(testDispatcher) {
        val repo = FakeRepo(testPrompts)
        val vm = SavedPromptsViewModel(
            observeSavedPromptsUseCase = ObserveSavedPromptsUseCase(repo),
            deleteSavedPromptUseCase = DeleteSavedPromptUseCase(repo),
        )
        val job = backgroundScope.launch(testDispatcher) { vm.prompts.collect {} }
        assertEquals(1, vm.prompts.value.size)
        assertEquals("1girl", vm.prompts.value[0].prompt)
        job.cancel()
    }

    @Test
    fun delete_calls_use_case() {
        val repo = FakeRepo(testPrompts)
        val vm = SavedPromptsViewModel(
            observeSavedPromptsUseCase = ObserveSavedPromptsUseCase(repo),
            deleteSavedPromptUseCase = DeleteSavedPromptUseCase(repo),
        )
        vm.delete(1L)
        assertTrue(repo.deletedId == 1L)
    }
}
