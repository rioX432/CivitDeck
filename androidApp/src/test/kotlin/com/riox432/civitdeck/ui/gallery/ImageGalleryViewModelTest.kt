package com.riox432.civitdeck.ui.gallery

import com.riox432.civitdeck.domain.model.Image
import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.domain.model.PageMetadata
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.ImageRepository
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import com.riox432.civitdeck.domain.usecase.GetImagesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.SavePromptUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ImageGalleryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun testImage(id: Long, width: Int = 512, height: Int = 768) = Image(
        id = id, url = "https://example.com/$id.png", hash = null,
        width = width, height = height, nsfw = false, nsfwLevel = NsfwLevel.None,
        createdAt = "2024-01-01", postId = null, username = "user",
        stats = null, meta = null,
    )

    private class FakeImageRepo(
        private val pages: List<PaginatedResult<Image>>,
    ) : ImageRepository {
        var callCount = 0

        override suspend fun getImages(
            modelId: Long?,
            modelVersionId: Long?,
            username: String?,
            sort: SortOrder?,
            period: TimePeriod?,
            nsfwLevel: NsfwLevel?,
            limit: Int?,
            cursor: String?,
        ): PaginatedResult<Image> {
            val result = pages.getOrElse(callCount) { pages.last() }
            callCount++
            return result
        }
    }

    private class FakeSavedPromptRepo : SavedPromptRepository {
        var savedMeta: ImageGenerationMeta? = null

        override suspend fun save(meta: ImageGenerationMeta, sourceImageUrl: String?) {
            savedMeta = meta
        }

        override fun observeAll() = flowOf(emptyList<com.riox432.civitdeck.domain.model.SavedPrompt>())
        override suspend fun delete(id: Long) = Unit
    }

    private fun fakePrefsRepo(
        nsfwFlow: MutableStateFlow<NsfwFilterLevel> = MutableStateFlow(NsfwFilterLevel.Off),
    ): UserPreferencesRepository = object : UserPreferencesRepository {
        override fun observeNsfwFilterLevel() = nsfwFlow
        override fun observeDefaultSortOrder() = flowOf(SortOrder.HighestRated)
        override fun observeDefaultTimePeriod() = flowOf(TimePeriod.AllTime)
        override fun observeGridColumns() = flowOf(2)
        override fun observeApiKey() = flowOf(null)
        override suspend fun setNsfwFilterLevel(level: NsfwFilterLevel) = Unit
        override suspend fun setDefaultSortOrder(sort: SortOrder) = Unit
        override suspend fun setDefaultTimePeriod(period: TimePeriod) = Unit
        override suspend fun setGridColumns(columns: Int) = Unit
        override suspend fun setApiKey(apiKey: String?) = Unit
        override suspend fun getApiKey(): String? = null
    }

    private fun createVm(
        imageRepo: FakeImageRepo,
        savedPromptRepo: FakeSavedPromptRepo = FakeSavedPromptRepo(),
        prefsRepo: UserPreferencesRepository = fakePrefsRepo(),
    ): ImageGalleryViewModel = ImageGalleryViewModel(
        modelVersionId = 100L,
        getImagesUseCase = GetImagesUseCase(imageRepo),
        savePromptUseCase = SavePromptUseCase(savedPromptRepo),
        observeNsfwFilterUseCase = ObserveNsfwFilterUseCase(prefsRepo),
    )

    @Test
    fun loads_initial_images() {
        val images = listOf(testImage(1L), testImage(2L))
        val repo = FakeImageRepo(
            listOf(PaginatedResult(images, PageMetadata(null, null))),
        )
        val vm = createVm(repo)
        assertEquals(2, vm.uiState.value.allImages.size)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.hasMore)
    }

    @Test
    fun loadMore_appends_images() {
        val page1 = PaginatedResult(
            listOf(testImage(1L)),
            PageMetadata(nextCursor = "c2", nextPage = null),
        )
        val page2 = PaginatedResult(
            listOf(testImage(2L)),
            PageMetadata(nextCursor = null, nextPage = null),
        )
        val repo = FakeImageRepo(listOf(page1, page2))
        val vm = createVm(repo)
        assertTrue(vm.uiState.value.hasMore)

        vm.loadMore()
        assertEquals(2, vm.uiState.value.allImages.size)
        assertFalse(vm.uiState.value.hasMore)
    }

    @Test
    fun error_state_on_failure() {
        val failingRepo = object : ImageRepository {
            override suspend fun getImages(
                modelId: Long?,
                modelVersionId: Long?,
                username: String?,
                sort: SortOrder?,
                period: TimePeriod?,
                nsfwLevel: NsfwLevel?,
                limit: Int?,
                cursor: String?,
            ) = error("API error")
        }
        val vm = ImageGalleryViewModel(
            modelVersionId = 1L,
            getImagesUseCase = GetImagesUseCase(failingRepo),
            savePromptUseCase = SavePromptUseCase(FakeSavedPromptRepo()),
            observeNsfwFilterUseCase = ObserveNsfwFilterUseCase(fakePrefsRepo()),
        )
        assertEquals("API error", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun onSortSelected_reloads() {
        val page1 = PaginatedResult(
            listOf(testImage(1L)),
            PageMetadata(null, null),
        )
        val page2 = PaginatedResult(
            listOf(testImage(10L), testImage(11L)),
            PageMetadata(null, null),
        )
        val repo = FakeImageRepo(listOf(page1, page2))
        val vm = createVm(repo)
        assertEquals(1, vm.uiState.value.allImages.size)

        vm.onSortSelected(SortOrder.Newest)
        assertEquals(SortOrder.Newest, vm.uiState.value.selectedSort)
        assertEquals(2, vm.uiState.value.allImages.size)
        assertEquals(10L, vm.uiState.value.allImages[0].id)
    }

    @Test
    fun onPeriodSelected_reloads() {
        val page1 = PaginatedResult(listOf(testImage(1L)), PageMetadata(null, null))
        val page2 = PaginatedResult(listOf(testImage(20L)), PageMetadata(null, null))
        val repo = FakeImageRepo(listOf(page1, page2))
        val vm = createVm(repo)

        vm.onPeriodSelected(TimePeriod.Month)
        assertEquals(TimePeriod.Month, vm.uiState.value.selectedPeriod)
        assertEquals(20L, vm.uiState.value.allImages[0].id)
    }

    @Test
    fun onAspectRatioSelected_filters_images() {
        // portrait: 512x768, landscape: 768x512, square: 512x512
        val images = listOf(
            testImage(1L, width = 512, height = 768),
            testImage(2L, width = 768, height = 512),
            testImage(3L, width = 512, height = 512),
        )
        val repo = FakeImageRepo(
            listOf(PaginatedResult(images, PageMetadata(null, null))),
        )
        val vm = createVm(repo)
        assertEquals(3, vm.uiState.value.images.size)

        vm.onAspectRatioSelected(com.riox432.civitdeck.domain.model.AspectRatioFilter.Portrait)
        assertEquals(1, vm.uiState.value.images.size)
        assertEquals(1L, vm.uiState.value.images[0].id)

        vm.onAspectRatioSelected(null)
        assertEquals(3, vm.uiState.value.images.size)
    }

    @Test
    fun onImageSelected_and_dismiss() {
        val repo = FakeImageRepo(
            listOf(PaginatedResult(listOf(testImage(1L)), PageMetadata(null, null))),
        )
        val vm = createVm(repo)
        assertNull(vm.uiState.value.selectedImageIndex)

        vm.onImageSelected(0)
        assertEquals(0, vm.uiState.value.selectedImageIndex)

        vm.onDismissViewer()
        assertNull(vm.uiState.value.selectedImageIndex)
    }

    @Test
    fun loadMore_noop_when_no_more_pages() {
        val repo = FakeImageRepo(
            listOf(PaginatedResult(listOf(testImage(1L)), PageMetadata(null, null))),
        )
        val vm = createVm(repo)
        assertFalse(vm.uiState.value.hasMore)
        vm.loadMore()
        assertEquals(1, repo.callCount) // only initial load
    }
}
