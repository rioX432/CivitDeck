package com.riox432.civitdeck.feature.search.presentation

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers how [SwipeDiscoveryViewModel] applies the user's NSFW filter level (the `nsfw`
 * request flag sent to `/models` and the client-side image filtering of loaded cards) and
 * how it pages through results with `nextCursor`, and how right swipes and Undo change Favorites.
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
        val favoriteRepo: FakeFavoriteRepository,
    )

    private fun TestScope.createViewModel(
        level: NsfwFilterLevel,
        models: List<Model> = listOf(testModel(id = SFW_MODEL_ID)),
        pages: List<PaginatedResult<Model>> = listOf(testPaginatedResult(items = models)),
        favoriteRepo: FakeFavoriteRepository = FakeFavoriteRepository(),
    ): TestDeps {
        // Defers the init-launched load until the test advances the scheduler.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val modelRepo = FakeModelRepository(pages)
        val nsfwPrefs = FakeContentFilterPreferencesRepository(level)
        val vm = SwipeDiscoveryViewModel(
            getDiscoveryModels = GetDiscoveryModelsUseCase(modelRepo),
            toggleFavorite = ToggleFavoriteUseCase(favoriteRepo),
            observeIsFavorite = ObserveIsFavoriteUseCase(favoriteRepo),
            observeNsfwFilter = ObserveNsfwFilterUseCase(nsfwPrefs),
        )
        return TestDeps(vm, modelRepo, nsfwPrefs, favoriteRepo)
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

    @Test
    fun prefetch_requestsNextPageWithCursorAndAppendsItsCards() = runTest {
        val deps = createViewModel(
            level = NsfwFilterLevel.Off,
            pages = listOf(
                testPaginatedResult(items = models(PREFETCH_IDS.take(4)), nextCursor = "c1"),
                testPaginatedResult(items = models(PREFETCH_IDS.drop(4)), nextCursor = null),
            ),
        )
        advanceUntilIdle()

        // Leaves 3 cards, which reaches the prefetch threshold.
        deps.vm.onSwipeLeft(deps.vm.state.value.cards.first())
        advanceUntilIdle()

        assertEquals(2, deps.modelRepo.getModelsCallCount)
        assertEquals("c1", deps.modelRepo.lastQuery!!.cursor)
        assertEquals(PREFETCH_IDS.drop(1), deps.vm.state.value.cards.map { it.id })
    }

    @Test
    fun pageOfDismissedCards_continuesToNextPageWithinOneLoad() = runTest {
        val firstPageIds = SKIP_AHEAD_IDS.take(4)
        val earlier = createViewModel(
            level = NsfwFilterLevel.Off,
            pages = listOf(testPaginatedResult(items = models(firstPageIds), nextCursor = null)),
        )
        advanceUntilIdle()
        earlier.vm.state.value.cards.forEach { earlier.vm.onSwipeLeft(it) }
        advanceUntilIdle()
        // Without a nextCursor the swipe-triggered prefetches must not refetch page one.
        assertEquals(1, earlier.modelRepo.getModelsCallCount)

        val deps = createViewModel(
            level = NsfwFilterLevel.Off,
            pages = listOf(
                testPaginatedResult(items = models(firstPageIds), nextCursor = "c1"),
                testPaginatedResult(items = models(SKIP_AHEAD_IDS.drop(4)), nextCursor = null),
            ),
        )
        advanceUntilIdle()

        assertEquals(2, deps.modelRepo.getModelsCallCount)
        assertEquals("c1", deps.modelRepo.lastQuery!!.cursor)
        assertEquals(SKIP_AHEAD_IDS.drop(4), deps.vm.state.value.cards.map { it.id })
    }

    @Test
    fun everyPageAlreadySeen_stopsAfterFiveRequests() = runTest {
        val earlier = createViewModel(NsfwFilterLevel.Off, models = models(listOf(BOUNDED_MODEL_ID)))
        advanceUntilIdle()
        earlier.vm.onSwipeLeft(earlier.vm.state.value.cards.single())
        advanceUntilIdle()

        // The fake serves this page, cursor included, for every request.
        val deps = createViewModel(
            level = NsfwFilterLevel.Off,
            pages = listOf(
                testPaginatedResult(items = models(listOf(BOUNDED_MODEL_ID)), nextCursor = "c1"),
            ),
        )
        advanceUntilIdle()

        assertEquals(5, deps.modelRepo.getModelsCallCount)
        assertEquals(emptyList(), deps.vm.state.value.cards)
    }

    @Test
    fun swipeRightOnFavorite_keepsItFavoritedThroughUndo() = runTest {
        val deps = createViewModel(
            level = NsfwFilterLevel.Off,
            models = models(listOf(ALREADY_FAVORITE_MODEL_ID)),
            favoriteRepo = FakeFavoriteRepository(isFavorite = true),
        )
        advanceUntilIdle()

        deps.vm.onSwipeRight(deps.vm.state.value.cards.single())
        advanceUntilIdle()
        assertEquals(0, deps.favoriteRepo.toggleCount)

        deps.vm.undoLastSwipe()
        advanceUntilIdle()
        assertEquals(0, deps.favoriteRepo.toggleCount)
        assertTrue(deps.favoriteRepo.isFavoriteFlow.value)
    }

    @Test
    fun swipeRightOnNonFavorite_addsItAndUndoRemovesIt() = runTest {
        val ids = listOf(ADDED_FAVORITE_MODEL_ID, ADDED_FAVORITE_MODEL_ID + 1)
        val deps = createViewModel(level = NsfwFilterLevel.Off, models = models(ids))
        advanceUntilIdle()

        deps.vm.onSwipeRight(deps.vm.state.value.cards.first())
        advanceUntilIdle()
        assertEquals(1, deps.favoriteRepo.toggleCount)
        assertTrue(deps.favoriteRepo.isFavoriteFlow.value)

        deps.vm.undoLastSwipe()
        advanceUntilIdle()
        assertEquals(2, deps.favoriteRepo.toggleCount)
        assertFalse(deps.favoriteRepo.isFavoriteFlow.value)
        assertEquals(ids, deps.vm.state.value.cards.map { it.id })
    }

    @Test
    fun undoBeforeFavoriteCheckCompletes_stillRemovesTheAddedFavorite() = runTest {
        val deps = createViewModel(level = NsfwFilterLevel.Off, models = models(listOf(IMMEDIATE_UNDO_MODEL_ID)))
        advanceUntilIdle()

        deps.vm.onSwipeRight(deps.vm.state.value.cards.single())
        deps.vm.undoLastSwipe()
        advanceUntilIdle()

        assertEquals(2, deps.favoriteRepo.toggleCount)
        assertFalse(deps.favoriteRepo.isFavoriteFlow.value)
    }

    @Test
    fun undoOfLeftSwipeAfterRightSwipe_keepsTheEarlierFavorite() = runTest {
        val ids = listOf(LEFT_SWIPE_MODEL_ID, LEFT_SWIPE_MODEL_ID + 1)
        val deps = createViewModel(level = NsfwFilterLevel.Off, models = models(ids))
        advanceUntilIdle()

        deps.vm.onSwipeRight(deps.vm.state.value.cards.first())
        deps.vm.onSwipeLeft(deps.vm.state.value.cards.first())
        deps.vm.undoLastSwipe()
        advanceUntilIdle()

        assertEquals(1, deps.favoriteRepo.toggleCount)
        assertTrue(deps.favoriteRepo.isFavoriteFlow.value)
        assertEquals(ids.drop(1), deps.vm.state.value.cards.map { it.id })
    }

    private fun models(ids: List<Long>): List<Model> = ids.map { testModel(id = it) }

    private companion object {
        // Distinct from other tests' IDs: the VM keeps dismissed IDs in a process-wide set.
        const val SFW_MODEL_ID = 71_001L
        const val EXPLICIT_MODEL_ID = 71_002L
        val PREFETCH_IDS = (73_001L..73_008L).toList()
        val SKIP_AHEAD_IDS = (74_001L..74_008L).toList()
        const val BOUNDED_MODEL_ID = 75_001L
        const val ALREADY_FAVORITE_MODEL_ID = 76_001L
        const val ADDED_FAVORITE_MODEL_ID = 76_101L
        const val IMMEDIATE_UNDO_MODEL_ID = 76_201L
        const val LEFT_SWIPE_MODEL_ID = 76_301L
    }
}
