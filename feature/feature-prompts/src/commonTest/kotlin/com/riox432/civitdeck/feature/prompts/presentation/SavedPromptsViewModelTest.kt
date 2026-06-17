package com.riox432.civitdeck.feature.prompts.presentation

import com.riox432.civitdeck.feature.prompts.domain.usecase.DeleteSavedPromptUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ObserveSavedPromptsUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ObserveTemplatesUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.SearchSavedPromptsUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ToggleTemplateUseCase
import com.riox432.civitdeck.testing.FakeSavedPromptRepository
import com.riox432.civitdeck.testing.testSavedPrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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

    private fun createViewModel(repo: FakeSavedPromptRepository) = SavedPromptsViewModel(
        observeSavedPromptsUseCase = ObserveSavedPromptsUseCase(repo),
        deleteSavedPromptUseCase = DeleteSavedPromptUseCase(repo),
        searchSavedPromptsUseCase = SearchSavedPromptsUseCase(repo),
        observeTemplatesUseCase = ObserveTemplatesUseCase(repo),
        toggleTemplateUseCase = ToggleTemplateUseCase(repo),
    )

    @Test
    fun prompts_flow_emits_list() = runTest(testDispatcher) {
        val repo = FakeSavedPromptRepository(prompts = listOf(testSavedPrompt(prompt = "1girl")))
        val vm = createViewModel(repo)
        val job = backgroundScope.launch(testDispatcher) { vm.prompts.collect {} }
        assertEquals(1, vm.prompts.value.size)
        assertEquals("1girl", vm.prompts.value[0].prompt)
        job.cancel()
    }

    @Test
    fun delete_calls_use_case() {
        val repo = FakeSavedPromptRepository(prompts = listOf(testSavedPrompt()))
        val vm = createViewModel(repo)
        vm.delete(1L)
        assertEquals(1L, repo.deletedId)
    }
}
