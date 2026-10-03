package com.riox432.civitdeck.feature.comfyui.presentation

import com.riox432.civitdeck.data.image.ImageSaver
import com.riox432.civitdeck.data.image.SaveGeneratedImageUseCase
import com.riox432.civitdeck.domain.model.ComfyUIGeneratedImage
import com.riox432.civitdeck.domain.model.ComfyUIGenerationMeta
import com.riox432.civitdeck.domain.model.ComfyUIHistoryPage
import com.riox432.civitdeck.domain.model.DatasetCollection
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.model.ImageSource
import com.riox432.civitdeck.domain.model.ShareHashtag
import com.riox432.civitdeck.domain.repository.ComfyUIHistoryRepository
import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository
import com.riox432.civitdeck.domain.repository.ShareHashtagRepository
import com.riox432.civitdeck.domain.usecase.AddImageToDatasetUseCase
import com.riox432.civitdeck.domain.usecase.AddShareHashtagUseCase
import com.riox432.civitdeck.domain.usecase.CreateDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveShareHashtagsUseCase
import com.riox432.civitdeck.domain.usecase.RemoveShareHashtagUseCase
import com.riox432.civitdeck.domain.usecase.ToggleShareHashtagUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUIHistoryUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ComfyUIHistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_requests_first_page() = runTest(dispatcher) {
        val repo = FakeHistoryRepository { page(hasMore = true) }
        val vm = createViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(PAGE), repo.requested)
        assertTrue(vm.uiState.value.canLoadOlder)
    }

    @Test
    fun loadOlder_requests_wider_window_and_takes_canLoadOlder_from_hasMore() = runTest(dispatcher) {
        val repo = FakeHistoryRepository { maxItems -> page(hasMore = maxItems == PAGE) }
        val vm = createViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.loadOlder()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(PAGE, 2 * PAGE), repo.requested)
        assertFalse(vm.uiState.value.canLoadOlder)
        assertFalse(vm.uiState.value.isLoadingOlder)
    }

    @Test
    fun loadOlder_makes_no_request_when_hasMore_is_false() = runTest(dispatcher) {
        val repo = FakeHistoryRepository { page(hasMore = false) }
        val vm = createViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.loadOlder()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(PAGE), repo.requested)
    }

    @Test
    fun refresh_after_loadOlder_refetches_the_wider_window() = runTest(dispatcher) {
        val repo = FakeHistoryRepository { page(hasMore = true) }
        val vm = createViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()
        vm.loadOlder()
        dispatcher.scheduler.advanceUntilIdle()

        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(PAGE, 2 * PAGE, 2 * PAGE), repo.requested)
    }

    @Test
    fun loadOlder_keeps_images_visible_and_leaves_isLoading_off() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeHistoryRepository { maxItems ->
            if (maxItems == PAGE) {
                page(hasMore = true, "newest")
            } else {
                gate.await()
                page(hasMore = false, "newest", "older")
            }
        }
        val vm = createViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.loadOlder()
        dispatcher.scheduler.advanceUntilIdle()

        val loading = vm.uiState.value
        assertTrue(loading.isLoadingOlder)
        assertFalse(loading.isLoading)
        assertEquals(listOf("newest"), loading.images.map { it.id })

        gate.complete(Unit)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("newest", "older"), vm.uiState.value.images.map { it.id })
        assertFalse(vm.uiState.value.isLoadingOlder)
    }

    @Test
    fun refresh_cancels_in_flight_loadOlder() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeHistoryRepository { maxItems ->
            if (maxItems == PAGE) {
                page(hasMore = true, "newest")
            } else {
                gate.await()
                page(hasMore = false, "stale")
            }
        }
        val vm = createViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()
        vm.loadOlder()
        dispatcher.scheduler.advanceUntilIdle()

        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()
        gate.complete(Unit)
        dispatcher.scheduler.advanceUntilIdle()

        // The cancelled wider fetch never committed, so refresh() re-fetched the first page.
        assertEquals(listOf(PAGE, 2 * PAGE, PAGE), repo.requested)
        val state = vm.uiState.value
        assertEquals(listOf("newest"), state.images.map { it.id })
        assertTrue(state.canLoadOlder)
        assertFalse(state.isLoadingOlder)
    }

    private fun createViewModel(repo: ComfyUIHistoryRepository): ComfyUIHistoryViewModel {
        val datasets = UnusedDatasetRepository()
        val hashtags = UnusedHashtagRepository()
        return ComfyUIHistoryViewModel(
            fetchHistory = FetchComfyUIHistoryUseCase(repo),
            saveImage = SaveGeneratedImageUseCase(HttpClient(MockEngine { respondOk() }), NoOpImageSaver),
            observeDatasetCollections = ObserveDatasetCollectionsUseCase(datasets),
            addImageToDataset = AddImageToDatasetUseCase(datasets),
            createDatasetCollection = CreateDatasetCollectionUseCase(datasets),
            observeShareHashtags = ObserveShareHashtagsUseCase(hashtags),
            addShareHashtag = AddShareHashtagUseCase(hashtags),
            removeShareHashtag = RemoveShareHashtagUseCase(hashtags),
            toggleShareHashtag = ToggleShareHashtagUseCase(hashtags),
        )
    }

    private companion object {
        const val PAGE = FetchComfyUIHistoryUseCase.HISTORY_PAGE_SIZE

        fun page(hasMore: Boolean, vararg ids: String) = ComfyUIHistoryPage(
            images = ids.map { image(it) },
            hasMore = hasMore,
        )

        fun image(id: String) = ComfyUIGeneratedImage(
            id = id,
            promptId = id,
            filename = "$id.png",
            subfolder = "",
            type = "output",
            imageUrl = "http://localhost/view?filename=$id.png",
            meta = ComfyUIGenerationMeta(),
        )
    }
}

private class FakeHistoryRepository(
    private val respond: suspend (maxItems: Int) -> ComfyUIHistoryPage,
) : ComfyUIHistoryRepository {
    val requested = mutableListOf<Int>()

    override fun fetchHistory(maxItems: Int): Flow<ComfyUIHistoryPage> = flow {
        requested += maxItems
        emit(respond(maxItems))
    }

    override fun fetchHistoryItem(promptId: String): Flow<List<ComfyUIGeneratedImage>> = error("unused")
}

private object NoOpImageSaver : ImageSaver {
    override suspend fun saveToGallery(imageBytes: ByteArray, filename: String): Boolean = true
}

private class UnusedDatasetRepository : DatasetCollectionRepository {
    override fun observeCollections(): Flow<List<DatasetCollection>> = emptyFlow()
    override suspend fun createCollection(name: String, description: String): Long = error("unused")
    override suspend fun renameCollection(id: Long, name: String) = error("unused")
    override suspend fun deleteCollection(id: Long) = error("unused")
    override fun observeImages(datasetId: Long): Flow<List<DatasetImage>> = error("unused")
    override suspend fun addImage(
        datasetId: Long,
        imageUrl: String,
        sourceType: ImageSource,
        trainable: Boolean,
        tags: List<String>,
    ): Long = error("unused")
    override suspend fun removeImage(imageId: Long) = error("unused")
    override suspend fun removeImages(imageIds: List<Long>) = error("unused")
    override suspend fun updateTrainable(imageId: Long, trainable: Boolean) = error("unused")
    override suspend fun updateLicenseNote(imageId: Long, licenseNote: String?) = error("unused")
    override suspend fun getNonTrainableImages(datasetId: Long): List<DatasetImage> = error("unused")
    override suspend fun updatePHash(imageId: Long, pHash: String?) = error("unused")
    override suspend fun markExcluded(imageId: Long, excluded: Boolean) = error("unused")
    override suspend fun updateDimensions(imageId: Long, width: Int, height: Int) = error("unused")
}

private class UnusedHashtagRepository : ShareHashtagRepository {
    override fun observeAll(): Flow<List<ShareHashtag>> = emptyFlow()
    override suspend fun addCustom(tag: String) = error("unused")
    override suspend fun remove(tag: String) = error("unused")
    override suspend fun setEnabled(tag: String, isEnabled: Boolean) = error("unused")
}
