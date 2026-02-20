package com.riox432.civitdeck.ui.creator

import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.Creator
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelStats
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.PageMetadata
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.usecase.GetCreatorModelsUseCase
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

    private fun testModel(id: Long) = Model(
        id = id, name = "Model $id", description = null,
        type = ModelType.Checkpoint, nsfw = false, tags = emptyList(), mode = null,
        creator = Creator("testuser", null, null, null),
        stats = ModelStats(100, 50, 10, 20, 4.5),
        modelVersions = emptyList(),
    )

    private class FakeRepo(
        private val pages: List<PaginatedResult<Model>>,
    ) : ModelRepository {
        var callCount = 0

        override suspend fun getModels(
            query: String?,
            tag: String?,
            type: ModelType?,
            sort: SortOrder?,
            period: TimePeriod?,
            baseModels: List<BaseModel>?,
            cursor: String?,
            limit: Int?,
            username: String?,
            nsfw: Boolean?,
        ): PaginatedResult<Model> {
            val result = pages.getOrElse(callCount) { pages.last() }
            callCount++
            return result
        }

        override suspend fun getModel(id: Long) = error("not used")
        override suspend fun getModelVersion(id: Long) = error("not used")
        override suspend fun getModelVersionByHash(hash: String) = error("not used")
    }

    @Test
    fun loads_initial_models() {
        val models = listOf(testModel(1L), testModel(2L))
        val repo = FakeRepo(
            listOf(PaginatedResult(models, PageMetadata(nextCursor = null, nextPage = null))),
        )
        val vm = CreatorProfileViewModel("testuser", GetCreatorModelsUseCase(repo))
        val state = vm.uiState.value
        assertEquals(2, state.models.size)
        assertFalse(state.isLoading)
        assertFalse(state.hasMore) // no nextCursor
    }

    @Test
    fun loadMore_appends_models() {
        val page1 = PaginatedResult(
            listOf(testModel(1L)),
            PageMetadata(nextCursor = "cursor2", nextPage = null),
        )
        val page2 = PaginatedResult(
            listOf(testModel(2L)),
            PageMetadata(nextCursor = null, nextPage = null),
        )
        val repo = FakeRepo(listOf(page1, page2))
        val vm = CreatorProfileViewModel("testuser", GetCreatorModelsUseCase(repo))
        assertEquals(1, vm.uiState.value.models.size)
        assertTrue(vm.uiState.value.hasMore)

        vm.loadMore()
        assertEquals(2, vm.uiState.value.models.size)
        assertFalse(vm.uiState.value.hasMore)
    }

    @Test
    fun error_state_on_failure() {
        val failingRepo = object : ModelRepository {
            override suspend fun getModels(
                query: String?,
                tag: String?,
                type: ModelType?,
                sort: SortOrder?,
                period: TimePeriod?,
                baseModels: List<BaseModel>?,
                cursor: String?,
                limit: Int?,
                username: String?,
                nsfw: Boolean?,
            ): PaginatedResult<Model> = error("Network error")
            override suspend fun getModel(id: Long) = error("not used")
            override suspend fun getModelVersion(id: Long) = error("not used")
            override suspend fun getModelVersionByHash(hash: String) = error("not used")
        }
        val vm = CreatorProfileViewModel("testuser", GetCreatorModelsUseCase(failingRepo))
        assertEquals("Network error", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun refresh_reloads_from_scratch() {
        val page1 = PaginatedResult(
            listOf(testModel(1L)),
            PageMetadata(nextCursor = "cursor2", nextPage = null),
        )
        val page2 = PaginatedResult(
            listOf(testModel(10L), testModel(11L)),
            PageMetadata(nextCursor = null, nextPage = null),
        )
        val repo = FakeRepo(listOf(page1, page2))
        val vm = CreatorProfileViewModel("testuser", GetCreatorModelsUseCase(repo))
        assertEquals(1, vm.uiState.value.models.size)

        vm.refresh()
        // After refresh, should have page2 content
        assertEquals(2, vm.uiState.value.models.size)
        assertEquals(10L, vm.uiState.value.models[0].id)
    }

    @Test
    fun loadMore_noop_when_no_more_pages() {
        val repo = FakeRepo(
            listOf(PaginatedResult(listOf(testModel(1L)), PageMetadata(null, null))),
        )
        val vm = CreatorProfileViewModel("testuser", GetCreatorModelsUseCase(repo))
        assertFalse(vm.uiState.value.hasMore)
        vm.loadMore() // should not call repo again
        assertEquals(1, repo.callCount) // only initial load
    }

    @Test
    fun username_is_set_in_state() {
        val repo = FakeRepo(
            listOf(PaginatedResult(emptyList(), PageMetadata(null, null))),
        )
        val vm = CreatorProfileViewModel("artist123", GetCreatorModelsUseCase(repo))
        assertEquals("artist123", vm.uiState.value.username)
    }
}
