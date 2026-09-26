package com.riox432.civitdeck.feature.search.presentation

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetDiscoveryModelsUseCase
import com.riox432.civitdeck.testing.FakeContentFilterPreferencesRepository
import com.riox432.civitdeck.testing.FakeFavoriteRepository
import com.riox432.civitdeck.testing.FakeModelRepository
import com.riox432.civitdeck.testing.testModel
import com.riox432.civitdeck.testing.testModelVersion
import com.riox432.civitdeck.testing.testPaginatedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers how [SwipeDiscoveryViewModel] applies the user's NSFW filter level: the `nsfw`
 * request flag sent to `/models` and the client-side image filtering of loaded cards.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SwipeDiscoveryViewModelTest {

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class TestDeps(
        val vm: SwipeDiscoveryViewModel,
        val modelRepo: FakeModelRepository,
        val nsfwPrefs: FakeContentFilterPreferencesRepository,
    )

    private fun TestScope.createViewModel(
        level: NsfwFilterLevel,
        models: List<Model> = listOf(testModel(id = SFW_MODEL_ID)),
    ): TestDeps {
        // Defers the init-launched load until the test advances the scheduler.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val modelRepo = FakeModelRepository(listOf(testPaginatedResult(items = models)))
        val nsfwPrefs = FakeContentFilterPreferencesRepository(level)
        val vm = SwipeDiscoveryViewModel(
            getDiscoveryModels = GetDiscoveryModelsUseCase(modelRepo),
            toggleFavorite = ToggleFavoriteUseCase(FakeFavoriteRepository()),
            observeNsfwFilter = ObserveNsfwFilterUseCase(nsfwPrefs),
        )
        return TestDeps(vm, modelRepo, nsfwPrefs)
    }

    @Test
    fun levelOff_requestsModelsWithNsfwFalse() = runTest {
        val deps = createViewModel(NsfwFilterLevel.Off)
        advanceUntilIdle()

        assertEquals(false, deps.modelRepo.lastQuery!!.nsfw)
    }

    @Test
    fun levelAll_requestsModelsWithNsfwTrue() = runTest {
        val deps = createViewModel(NsfwFilterLevel.All)
        advanceUntilIdle()

        assertEquals(true, deps.modelRepo.lastQuery!!.nsfw)
    }

    @Test
    fun levelOff_dropsModelWhoseOnlyImageIsX() = runTest {
        val explicitModel = testModel(
            id = EXPLICIT_MODEL_ID,
            modelVersions = listOf(
                testModelVersion(modelId = EXPLICIT_MODEL_ID, nsfwLevel = NsfwLevel.X),
            ),
        )
        val deps = createViewModel(
            level = NsfwFilterLevel.Off,
            models = listOf(testModel(id = SFW_MODEL_ID), explicitModel),
        )
        advanceUntilIdle()

        assertEquals(listOf(SFW_MODEL_ID), deps.vm.state.value.cards.map { it.id })
    }

    @Test
    fun nextLoad_usesLevelChangedAfterPreviousLoad() = runTest {
        val deps = createViewModel(NsfwFilterLevel.Off)
        advanceUntilIdle()

        deps.nsfwPrefs.nsfwFilterLevelFlow.value = NsfwFilterLevel.All
        deps.vm.loadModels()
        advanceUntilIdle()

        assertEquals(2, deps.modelRepo.getModelsCallCount)
        assertEquals(true, deps.modelRepo.lastQuery!!.nsfw)
    }

    private companion object {
        // Distinct from other tests' IDs: the VM keeps dismissed IDs in a process-wide set.
        const val SFW_MODEL_ID = 71_001L
        const val EXPLICIT_MODEL_ID = 71_002L
    }
}
