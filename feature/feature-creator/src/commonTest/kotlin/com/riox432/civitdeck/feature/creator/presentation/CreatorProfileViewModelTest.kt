package com.riox432.civitdeck.feature.creator.presentation

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelSearchQuery
import com.riox432.civitdeck.domain.model.PageMetadata
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.usecase.FollowCreatorUseCase
import com.riox432.civitdeck.domain.usecase.IsFollowingCreatorUseCase
import com.riox432.civitdeck.domain.usecase.UnfollowCreatorUseCase
import com.riox432.civitdeck.feature.creator.domain.usecase.GetCreatorModelsUseCase
import com.riox432.civitdeck.testing.FakeCreatorFollowRepository
import com.riox432.civitdeck.testing.testModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreatorProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun creatorModel(id: Long) = testModel(id = id, name = "Model $id", modelVersions = emptyList())

    private class FakeModelRepo(
        private val pages: List<PaginatedResult<Model>>,
    ) : ModelRepository {
        var callCount = 0

        override suspend fun getModels(query: ModelSearchQuery): PaginatedResult<Model> {
            val result = pages.getOrElse(callCount) { pages.last() }
            callCount++
            return result
        }

        override suspend fun getModel(id: Long) = error("not used")
        override suspend fun getModelVersion(id: Long) = error("not used")
        override suspend fun getModelVersionByHash(hash: String) = error("not used")
        override suspend fun getModelLicense(versionId: Long) = null
    }

    private fun createViewModel(
        username: String,
        repo: ModelRepository,
        followRepo: FakeCreatorFollowRepository = FakeCreatorFollowRepository(),
    ) = CreatorProfileViewModel(
        username = username,
        getCreatorModelsUseCase = GetCreatorModelsUseCase(repo),
        isFollowingCreatorUseCase = IsFollowingCreatorUseCase(followRepo),
        followCreatorUseCase = FollowCreatorUseCase(followRepo),
        unfollowCreatorUseCase = UnfollowCreatorUseCase(followRepo),
    )

    @Test
    fun loads_initial_models() {
        val models = listOf(creatorModel(1L), creatorModel(2L))
        val repo = FakeModelRepo(
            listOf(PaginatedResult(models, PageMetadata(nextCursor = null, nextPage = null))),
        )
        val vm = createViewModel("testuser", repo)
        val state = vm.uiState.value
        assertEquals(2, state.models.size)
        assertFalse(state.isLoading)
        assertFalse(state.hasMore)
    }

    @Test
    fun loadMore_appends_models() {
        val page1 = PaginatedResult(
            listOf(creatorModel(1L)),
            PageMetadata(nextCursor = "cursor2", nextPage = null),
        )
        val page2 = PaginatedResult(
            listOf(creatorModel(2L)),
            PageMetadata(nextCursor = null, nextPage = null),
        )
        val repo = FakeModelRepo(listOf(page1, page2))
        val vm = createViewModel("testuser", repo)
        assertEquals(1, vm.uiState.value.models.size)
        assertTrue(vm.uiState.value.hasMore)

        vm.loadMore()
        assertEquals(2, vm.uiState.value.models.size)
        assertFalse(vm.uiState.value.hasMore)
    }

    @Test
    fun error_state_on_failure() {
        val failingRepo = object : ModelRepository {
            override suspend fun getModels(query: ModelSearchQuery): PaginatedResult<Model> =
                error("Network error")
            override suspend fun getModel(id: Long) = error("not used")
            override suspend fun getModelVersion(id: Long) = error("not used")
            override suspend fun getModelVersionByHash(hash: String) = error("not used")
            override suspend fun getModelLicense(versionId: Long) = null
        }
        val vm = createViewModel("testuser", failingRepo)
        assertEquals("Network error", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun refresh_reloads_from_scratch() {
        val page1 = PaginatedResult(
            listOf(creatorModel(1L)),
            PageMetadata(nextCursor = "cursor2", nextPage = null),
        )
        val page2 = PaginatedResult(
            listOf(creatorModel(10L), creatorModel(11L)),
            PageMetadata(nextCursor = null, nextPage = null),
        )
        val repo = FakeModelRepo(listOf(page1, page2))
        val vm = createViewModel("testuser", repo)
        assertEquals(1, vm.uiState.value.models.size)

        vm.refresh()
        assertEquals(2, vm.uiState.value.models.size)
        assertEquals(10L, vm.uiState.value.models[0].id)
    }

    @Test
    fun loadMore_noop_when_no_more_pages() {
        val repo = FakeModelRepo(
            listOf(PaginatedResult(listOf(creatorModel(1L)), PageMetadata(null, null))),
        )
        val vm = createViewModel("testuser", repo)
        assertFalse(vm.uiState.value.hasMore)
        vm.loadMore()
        assertEquals(1, repo.callCount)
    }

    @Test
    fun username_is_set_in_state() {
        val repo = FakeModelRepo(
            listOf(PaginatedResult(emptyList(), PageMetadata(null, null))),
        )
        val vm = createViewModel("artist123", repo)
        assertEquals("artist123", vm.uiState.value.username)
    }
}
