package com.riox432.civitdeck.feature.detail.presentation

import app.cash.turbine.test
import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelCollection
import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.model.ModelNote
import com.riox432.civitdeck.domain.model.ModelSearchQuery
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.PersonalTag
import com.riox432.civitdeck.domain.repository.CollectionRepository
import com.riox432.civitdeck.domain.repository.ContentFilterPreferencesRepository
import com.riox432.civitdeck.domain.repository.ModelDownloadRepository
import com.riox432.civitdeck.domain.repository.ModelEmbeddingRepository
import com.riox432.civitdeck.domain.repository.ModelNoteRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.repository.ThumbnailDownloader
import com.riox432.civitdeck.domain.usecase.AddModelToCollectionUseCase
import com.riox432.civitdeck.domain.usecase.AddPersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.CancelDownloadUseCase
import com.riox432.civitdeck.domain.usecase.CreateCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeleteModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.EmbedImageUseCase
import com.riox432.civitdeck.domain.usecase.EmbedOnBrowseUseCase
import com.riox432.civitdeck.domain.usecase.EnqueueDownloadUseCase
import com.riox432.civitdeck.domain.usecase.EnrichModelImagesUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelDownloadsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObservePersonalTagsUseCase
import com.riox432.civitdeck.domain.usecase.ObservePowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.RemoveModelFromCollectionUseCase
import com.riox432.civitdeck.domain.usecase.RemovePersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.SaveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
import com.riox432.civitdeck.domain.util.ApplicationScope
import com.riox432.civitdeck.domain.util.SystemStatsProvider
import com.riox432.civitdeck.testing.FakeAppBehaviorPreferencesRepository
import com.riox432.civitdeck.testing.FakeBrowsingHistoryRepository
import com.riox432.civitdeck.testing.FakeContentFilterPreferencesRepository
import com.riox432.civitdeck.testing.FakeFavoriteRepository
import com.riox432.civitdeck.testing.FakeImageEmbeddingModel
import com.riox432.civitdeck.testing.clearForTest
import com.riox432.civitdeck.testing.testApplicationScope
import com.riox432.civitdeck.testing.testModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ModelDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region Fake repositories (detail-specific; shared fakes used where reusable)

    private class FakeModelRepo(val model: Model) : ModelRepository {
        var getModelCalled = false

        override suspend fun getModels(query: ModelSearchQuery): PaginatedResult<Model> =
            error("not used")

        override suspend fun getModel(id: Long): Model {
            getModelCalled = true
            return model
        }

        override suspend fun getModelVersion(id: Long) = model.modelVersions.first()
        override suspend fun getModelVersionByHash(hash: String): ModelVersion = error("not used")
        override suspend fun getModelLicense(versionId: Long) = null
    }

    private class FakeCollectionRepo : CollectionRepository {
        override fun observeCollections() = flowOf(emptyList<ModelCollection>())
        override fun observeModelsInCollection(collectionId: Long) = flowOf(
            emptyList<com.riox432.civitdeck.domain.model.FavoriteModelSummary>(),
        )
        override fun observeCollectionIdsForModel(modelId: Long) = flowOf(emptyList<Long>())
        override suspend fun createCollection(name: String) = 1L
        override suspend fun renameCollection(id: Long, name: String) = Unit
        override suspend fun deleteCollection(id: Long) = Unit
        override suspend fun addModelToCollection(collectionId: Long, model: Model) = Unit
        override suspend fun removeModelFromCollection(collectionId: Long, modelId: Long) = Unit
        override suspend fun bulkMoveModels(
            fromCollectionId: Long,
            toCollectionId: Long,
            modelIds: List<Long>,
        ) = Unit
        override suspend fun bulkRemoveModels(collectionId: Long, modelIds: List<Long>) = Unit
    }

    private class FakeNoteRepo : ModelNoteRepository {
        override fun observeNoteForModel(modelId: Long) = flowOf<ModelNote?>(null)
        override suspend fun saveNote(modelId: Long, noteText: String) = Unit
        override suspend fun deleteNote(modelId: Long) = Unit
        override fun observeTagsForModel(modelId: Long) = flowOf(emptyList<PersonalTag>())
        override suspend fun addTag(modelId: Long, tag: String) = Unit
        override suspend fun removeTag(modelId: Long, tag: String) = Unit
        override suspend fun getAllTags() = emptyList<String>()
        override suspend fun getModelIdsByTag(tag: String) = emptyList<Long>()
    }

    private class FakeDownloadRepo : ModelDownloadRepository {
        override suspend fun enqueueDownload(download: ModelDownload) = 1L
        override fun observeAllDownloads() = flowOf(emptyList<ModelDownload>())
        override fun observeDownloadsForModel(modelId: Long) = flowOf(emptyList<ModelDownload>())
        override suspend fun getDownloadById(id: Long): ModelDownload? = null
        override suspend fun getDownloadByFileId(fileId: Long): ModelDownload? = null
        override suspend fun updateStatus(id: Long, status: DownloadStatus, errorMessage: String?) = Unit
        override suspend fun updateProgress(id: Long, downloadedBytes: Long) = Unit
        override suspend fun updateDestinationPath(id: Long, path: String) = Unit
        override suspend fun deleteDownload(id: Long) = Unit
        override suspend fun updateHashVerified(id: Long, verified: Boolean) = Unit
        override suspend fun clearCompletedDownloads() = Unit
    }

    private class NoOpEmbeddingRepo : ModelEmbeddingRepository {
        override suspend fun get(modelId: Long) = null
        override suspend fun count(embeddingModel: String) = 0
        override suspend fun cache(embedding: com.riox432.civitdeck.domain.model.ModelEmbedding) = Unit
        override suspend fun findSimilar(
            query: FloatArray,
            embeddingModel: String,
            limit: Int,
            excludeModelId: Long?,
        ) = emptyList<com.riox432.civitdeck.domain.model.SimilarModelHit>()
        override suspend fun deleteStale(keepModel: String) = 0
        override suspend fun clear() = Unit
    }

    private class NoOpDownloader : ThumbnailDownloader {
        override suspend fun download(url: String) = byteArrayOf()
    }

    // endregion

    private class TestDeps(
        val vm: ModelDetailViewModel,
        val modelRepo: FakeModelRepo,
        val favRepo: FakeFavoriteRepository,
        val browsingRepo: FakeBrowsingHistoryRepository,
    )

    private fun createViewModel(
        scope: TestScope,
        model: Model = testModel(),
        isFavorite: Boolean = false,
        browsingRepo: FakeBrowsingHistoryRepository = FakeBrowsingHistoryRepository(),
        prefsRepo: ContentFilterPreferencesRepository = FakeContentFilterPreferencesRepository(),
        appScope: ApplicationScope = testApplicationScope(scope),
    ): TestDeps {
        val modelRepo = FakeModelRepo(model)
        val favRepo = FakeFavoriteRepository(isFavorite = isFavorite)
        val collectionRepo = FakeCollectionRepo()
        val noteRepo = FakeNoteRepo()
        val powerUserRepo = FakeAppBehaviorPreferencesRepository()
        val downloadRepo = FakeDownloadRepo()

        val modelUseCases = ModelUseCases(
            getModelDetail = GetModelDetailUseCase(modelRepo),
            observeIsFavorite = ObserveIsFavoriteUseCase(favRepo),
            toggleFavorite = ToggleFavoriteUseCase(favRepo),
            trackModelView = TrackModelViewUseCase(browsingRepo),
            enrichModelImages = EnrichModelImagesUseCase(modelRepo),
            embedOnBrowse = EmbedOnBrowseUseCase(
                NoOpEmbeddingRepo(),
                EmbedImageUseCase(FakeImageEmbeddingModel()),
                NoOpDownloader(),
            ),
            observeNsfwFilter = ObserveNsfwFilterUseCase(prefsRepo),
            observePowerUserMode = ObservePowerUserModeUseCase(powerUserRepo),
        )
        val collectionUseCases = CollectionUseCases(
            observeCollections = ObserveCollectionsUseCase(collectionRepo),
            observeModelCollections = ObserveModelCollectionsUseCase(collectionRepo),
            addModelToCollection = AddModelToCollectionUseCase(collectionRepo),
            removeModelFromCollection = RemoveModelFromCollectionUseCase(collectionRepo),
            createCollection = CreateCollectionUseCase(collectionRepo),
        )
        val notesTagsUseCases = NotesTagsUseCases(
            observeModelNote = ObserveModelNoteUseCase(noteRepo),
            saveModelNote = SaveModelNoteUseCase(noteRepo),
            deleteModelNote = DeleteModelNoteUseCase(noteRepo),
            observePersonalTags = ObservePersonalTagsUseCase(noteRepo),
            addPersonalTag = AddPersonalTagUseCase(noteRepo),
            removePersonalTag = RemovePersonalTagUseCase(noteRepo),
        )
        val downloadUseCases = DownloadUseCases(
            observeModelDownloads = ObserveModelDownloadsUseCase(downloadRepo),
            enqueueDownload = EnqueueDownloadUseCase(downloadRepo),
            cancelDownload = CancelDownloadUseCase(downloadRepo),
        )

        val vm = ModelDetailViewModel(
            modelId = model.id,
            modelUseCases = modelUseCases,
            collectionUseCases = collectionUseCases,
            notesTagsUseCases = notesTagsUseCases,
            downloadUseCases = downloadUseCases,
            systemStatsProvider = SystemStatsProvider { null },
            appScope = appScope,
        )
        return TestDeps(vm, modelRepo, favRepo, browsingRepo)
    }

    @Test
    fun loads_model_on_init() = runTest {
        val deps = createViewModel(this)
        val state = deps.vm.uiState.value
        assertNull(state.error)
        assertEquals("Test Model", state.model?.name)
        assertTrue(deps.modelRepo.getModelCalled)
    }

    @Test
    fun rating_is_retained_after_reviews_removed() = runTest {
        // Reviews list/submit were removed (#991), but Model.stats.rating comes from the
        // REST /models stats object (not the removed tRPC resourceReview path) and must still render.
        val deps = createViewModel(this)
        assertEquals(4.5, deps.vm.uiState.value.model?.stats?.rating)
    }

    @Test
    fun observes_favorite_state() = runTest {
        val deps = createViewModel(this, isFavorite = true)
        deps.vm.uiState.test {
            assertTrue(awaitItem().isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggle_favorite_calls_use_case() = runTest {
        val deps = createViewModel(this)
        deps.vm.onFavoriteToggle()
        assertEquals(1, deps.favRepo.toggleCount)
    }

    @Test
    fun on_cleared_tracks_end_view_via_application_scope() = runTest {
        // loadModel() sets viewStartTimeMs on init; clearing triggers onCleared -> trackEndView,
        // which launches into the injected ApplicationScope (this TestScope).
        val deps = createViewModel(this)
        deps.vm.clearForTest()
        // trackEndView launches into the injected ApplicationScope (this TestScope);
        // drain its scheduled work before asserting.
        advanceUntilIdle()
        assertEquals(1L, deps.browsingRepo.endViewModelId)
        assertNotNull(deps.browsingRepo.endViewDurationMs)
        assertTrue(requireNotNull(deps.browsingRepo.endViewDurationMs) >= 0L)
    }

    @Test
    fun version_selection_updates_state() = runTest {
        val model = testModel().let { m ->
            m.copy(
                modelVersions = m.modelVersions + ModelVersion(
                    id = 20L, modelId = m.id, name = "v2.0", description = null,
                    createdAt = "2024-02-01", baseModel = "SD 1.5",
                    trainedWords = emptyList(), downloadUrl = "https://example.com",
                    files = emptyList(), images = emptyList<ModelImage>(), stats = null,
                ),
            )
        }
        val deps = createViewModel(this, model = model)
        deps.vm.onVersionSelected(1)
        assertEquals(1, deps.vm.uiState.value.selectedVersionIndex)
    }

    @Test
    fun nsfw_filter_change_propagates_to_state() = runTest {
        val prefs = FakeContentFilterPreferencesRepository(NsfwFilterLevel.Off)
        val deps = createViewModel(this, prefsRepo = prefs)
        assertEquals(NsfwFilterLevel.Off, deps.vm.uiState.value.nsfwFilterLevel)
        prefs.nsfwFilterLevelFlow.value = NsfwFilterLevel.All
        assertEquals(NsfwFilterLevel.All, deps.vm.uiState.value.nsfwFilterLevel)
    }
}
