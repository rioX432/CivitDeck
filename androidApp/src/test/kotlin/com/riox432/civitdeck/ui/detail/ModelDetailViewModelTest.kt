package com.riox432.civitdeck.ui.detail

import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.Creator
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelCollection
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.model.ModelNote
import com.riox432.civitdeck.domain.model.ModelStats
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.PersonalTag
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository
import com.riox432.civitdeck.domain.repository.ContentFilterPreferencesRepository
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import com.riox432.civitdeck.domain.repository.ModelNoteRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.usecase.AddModelToCollectionUseCase
import com.riox432.civitdeck.domain.usecase.AddPersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.CreateCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeleteModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.EnrichModelImagesUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObservePersonalTagsUseCase
import com.riox432.civitdeck.domain.usecase.RemoveModelFromCollectionUseCase
import com.riox432.civitdeck.domain.usecase.RemovePersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.SaveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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

    private fun testModel(id: Long = 1L) = Model(
        id = id,
        name = "Test Model",
        description = "A test model",
        type = ModelType.Checkpoint,
        nsfw = false,
        tags = listOf("anime"),
        mode = null,
        creator = Creator("testuser", null, null, null),
        stats = ModelStats(100, 50, 10, 20, 4.5),
        modelVersions = listOf(
            ModelVersion(
                id = 10L, modelId = id, name = "v1.0", description = null,
                createdAt = "2024-01-01", baseModel = "SD 1.5",
                trainedWords = emptyList(), downloadUrl = "https://example.com",
                files = emptyList(),
                images = listOf(
                    ModelImage(
                        url = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/uuid1/photo.jpeg",
                        nsfw = false,
                        nsfwLevel = NsfwLevel.None,
                        width = 512,
                        height = 512,
                        hash = null,
                        meta = null,
                    ),
                ),
                stats = null,
            ),
        ),
    )

    private class FakeModelRepo(val model: Model) : ModelRepository {
        var getModelCalled = false

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
        ): PaginatedResult<Model> = error("not used")

        override suspend fun getModel(id: Long): Model {
            getModelCalled = true
            return model
        }

        override suspend fun getModelVersion(id: Long) = model.modelVersions.first()
        override suspend fun getModelVersionByHash(hash: String): ModelVersion =
            error("not used")
        override suspend fun getModelLicense(versionId: Long) = null
    }

    private class FakeFavoriteRepo(
        private val isFav: MutableStateFlow<Boolean> = MutableStateFlow(false),
    ) : FavoriteRepository {
        var toggleCalled = false

        override fun observeFavorites() =
            flowOf(emptyList<com.riox432.civitdeck.domain.model.FavoriteModelSummary>())
        override fun observeIsFavorite(modelId: Long): Flow<Boolean> = isFav
        override suspend fun toggleFavorite(model: Model) { toggleCalled = true }
        override suspend fun addFavorite(model: Model) = error("not used")
        override suspend fun removeFavorite(modelId: Long) = error("not used")
        override suspend fun getAllFavoriteIds() = error("not used")
        override suspend fun getFavoriteTypeCounts() = error("not used")
    }

    private class FakeBrowsingRepo : BrowsingHistoryRepository {
        var trackCalled = false
        override suspend fun trackView(
            modelId: Long,
            modelName: String,
            modelType: String,
            creatorName: String?,
            thumbnailUrl: String?,
            tags: List<String>,
        ) { trackCalled = true }
        override suspend fun getRecentTypes(limit: Int) = error("not used")
        override suspend fun getRecentCreators(limit: Int) = error("not used")
        override suspend fun getRecentTags(limit: Int) = error("not used")
        override suspend fun getRecentModelIds(limit: Int) = error("not used")
        override suspend fun getAllViewedModelIds() = error("not used")
        override suspend fun clearAll() = error("not used")
        override suspend fun deleteById(historyId: Long) = error("not used")
        override fun observeRecentlyViewed(limit: Int) = error("not used")
        override suspend fun cleanup(cutoffMillis: Long, maxEntries: Int) = error("not used")
        override suspend fun getWeightedTypes(limit: Int) = error("not used")
        override suspend fun getWeightedTags(limit: Int) = error("not used")
        override suspend fun getWeightedCreators(limit: Int) = error("not used")
        override suspend fun updateViewDuration(modelId: Long, durationMs: Long) = Unit
        override suspend fun trackInteraction(
            modelId: Long,
            interactionType: com.riox432.civitdeck.domain.model.InteractionType,
        ) = Unit
        override suspend fun getAverageViewDurationMs(): Long? = null
        override suspend fun getRecommendationClickCount(sinceMillis: Long) = 0
        override suspend fun getInteractionCountByType(
            type: com.riox432.civitdeck.domain.model.InteractionType,
            sinceMillis: Long,
        ) = 0
    }

    private class FakePrefsRepo : ContentFilterPreferencesRepository {
        override fun observeNsfwFilterLevel() = flowOf(NsfwFilterLevel.Off)
        override suspend fun setNsfwFilterLevel(level: NsfwFilterLevel) = error("not used")
        override fun observeNsfwBlurSettings() = error("not used")
        override suspend fun setNsfwBlurSettings(
            settings: com.riox432.civitdeck.domain.model.NsfwBlurSettings
        ) = error("not used")
    }

    private class FakeCollectionRepo :
        com.riox432.civitdeck.domain.repository.CollectionRepository {
        override fun observeCollections() = flowOf(emptyList<ModelCollection>())
        override fun observeModelsInCollection(collectionId: Long) = flowOf(
            emptyList<com.riox432.civitdeck.domain.model.FavoriteModelSummary>(),
        )
        override fun observeCollectionIdsForModel(modelId: Long) = flowOf(emptyList<Long>())
        override suspend fun createCollection(name: String) = 1L
        override suspend fun renameCollection(id: Long, name: String) = Unit
        override suspend fun deleteCollection(id: Long) = Unit
        override suspend fun addModelToCollection(collectionId: Long, model: Model) = Unit
        override suspend fun removeModelFromCollection(
            collectionId: Long,
            modelId: Long,
        ) = Unit
        override suspend fun bulkMoveModels(
            fromCollectionId: Long,
            toCollectionId: Long,
            modelIds: List<Long>,
        ) = Unit
        override suspend fun bulkRemoveModels(
            collectionId: Long,
            modelIds: List<Long>,
        ) = Unit
    }

    private class FakeNoteRepo : ModelNoteRepository {
        override fun observeNoteForModel(modelId: Long) = flowOf<ModelNote?>(null)
        override suspend fun saveNote(modelId: Long, noteText: String) = Unit
        override suspend fun deleteNote(modelId: Long) = Unit
        override fun observeTagsForModel(modelId: Long) =
            flowOf(emptyList<PersonalTag>())
        override suspend fun addTag(modelId: Long, tag: String) = Unit
        override suspend fun removeTag(modelId: Long, tag: String) = Unit
        override suspend fun getAllTags() = emptyList<String>()
        override suspend fun getModelIdsByTag(tag: String) = emptyList<Long>()
    }

    private class FakePowerUserRepo :
        com.riox432.civitdeck.domain.repository.AppBehaviorPreferencesRepository {
        override fun observePowerUserMode() = flowOf(false)
        override suspend fun setPowerUserMode(enabled: Boolean) = Unit
        override fun observeNotificationsEnabled() = flowOf(false)
        override suspend fun setNotificationsEnabled(enabled: Boolean) = Unit
        override fun observePollingInterval() =
            flowOf(com.riox432.civitdeck.domain.model.PollingInterval.Off)
        override suspend fun setPollingInterval(
            interval: com.riox432.civitdeck.domain.model.PollingInterval,
        ) = Unit
        override fun observeSeenTutorialVersion() = flowOf(0)
        override suspend fun setSeenTutorialVersion(version: Int) = Unit
        override fun observeCustomNavShortcuts() =
            flowOf(emptyList<com.riox432.civitdeck.domain.model.NavShortcut>())
        override suspend fun setCustomNavShortcuts(
            items: List<com.riox432.civitdeck.domain.model.NavShortcut>,
        ) = Unit
    }

    @Suppress("LongMethod")
    private fun createViewModel(
        model: Model = testModel(),
        isFavorite: Boolean = false,
    ): Triple<
        com.riox432.civitdeck.ui.detail.ModelDetailViewModel,
        FakeModelRepo,
        FakeFavoriteRepo,
        > {
        val modelRepo = FakeModelRepo(model)
        val favRepo = FakeFavoriteRepo(MutableStateFlow(isFavorite))
        val browsingRepo = FakeBrowsingRepo()
        val prefsRepo = FakePrefsRepo()
        val collectionRepo = FakeCollectionRepo()
        val noteRepo = FakeNoteRepo()
        val powerUserRepo = FakePowerUserRepo()

        val vm = com.riox432.civitdeck.ui.detail.ModelDetailViewModel(
            modelId = model.id,
            getModelDetailUseCase = GetModelDetailUseCase(modelRepo),
            observeIsFavoriteUseCase = ObserveIsFavoriteUseCase(favRepo),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(favRepo),
            trackModelViewUseCase = TrackModelViewUseCase(browsingRepo),
            observeNsfwFilterUseCase = ObserveNsfwFilterUseCase(prefsRepo),
            enrichModelImagesUseCase = EnrichModelImagesUseCase(modelRepo),
            observeCollectionsUseCase = ObserveCollectionsUseCase(collectionRepo),
            observeModelCollectionsUseCase = ObserveModelCollectionsUseCase(collectionRepo),
            addModelToCollectionUseCase = AddModelToCollectionUseCase(collectionRepo),
            removeModelFromCollectionUseCase = RemoveModelFromCollectionUseCase(collectionRepo),
            createCollectionUseCase = CreateCollectionUseCase(collectionRepo),
            observePowerUserModeUseCase = com.riox432.civitdeck.domain.usecase.ObservePowerUserModeUseCase(
                powerUserRepo
            ),
            observeModelNoteUseCase = ObserveModelNoteUseCase(noteRepo),
            saveModelNoteUseCase = SaveModelNoteUseCase(noteRepo),
            deleteModelNoteUseCase = DeleteModelNoteUseCase(noteRepo),
            observePersonalTagsUseCase = ObservePersonalTagsUseCase(noteRepo),
            addPersonalTagUseCase = AddPersonalTagUseCase(noteRepo),
            removePersonalTagUseCase = RemovePersonalTagUseCase(noteRepo),
        )
        return Triple(vm, modelRepo, favRepo)
    }

    @Test
    fun loads_model_on_init() {
        val (vm, modelRepo, _) = createViewModel()
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("Test Model", state.model?.name)
        assertTrue(modelRepo.getModelCalled)
    }

    @Test
    fun observes_favorite_state() {
        val (vm, _, _) = createViewModel(isFavorite = true)
        assertTrue(vm.uiState.value.isFavorite)
    }

    @Test
    fun toggle_favorite_calls_use_case() {
        val (vm, _, favRepo) = createViewModel()
        vm.onFavoriteToggle()
        assertTrue(favRepo.toggleCalled)
    }

    @Test
    fun version_selection_updates_state() {
        val model = testModel().let { m ->
            m.copy(
                modelVersions = m.modelVersions + ModelVersion(
                    id = 20L, modelId = m.id, name = "v2.0", description = null,
                    createdAt = "2024-02-01", baseModel = "SD 1.5",
                    trainedWords = emptyList(), downloadUrl = "https://example.com",
                    files = emptyList(), images = emptyList(), stats = null,
                ),
            )
        }
        val (vm, _, _) = createViewModel(model = model)
        vm.onVersionSelected(1)
        assertEquals(1, vm.uiState.value.selectedVersionIndex)
    }
}
