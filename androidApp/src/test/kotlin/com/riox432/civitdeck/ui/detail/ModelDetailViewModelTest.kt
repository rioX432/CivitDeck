package com.riox432.civitdeck.ui.detail

import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.Creator
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.model.ModelStats
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import com.riox432.civitdeck.domain.usecase.EnrichModelImagesUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
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
            modelType: String,
            creatorName: String?,
            tags: List<String>,
        ) { trackCalled = true }
        override suspend fun getRecentTypes(limit: Int) = error("not used")
        override suspend fun getRecentCreators(limit: Int) = error("not used")
        override suspend fun getRecentTags(limit: Int) = error("not used")
        override suspend fun getRecentModelIds(limit: Int) = error("not used")
        override suspend fun getAllViewedModelIds() = error("not used")
        override suspend fun clearAll() = error("not used")
    }

    @Suppress("TooManyFunctions")
    private class FakePrefsRepo : UserPreferencesRepository {
        override fun observeNsfwFilterLevel() = flowOf(NsfwFilterLevel.Off)
        override suspend fun setNsfwFilterLevel(level: NsfwFilterLevel) = error("not used")
        override fun observeDefaultSortOrder(): Flow<SortOrder> = error("not used")
        override suspend fun setDefaultSortOrder(sort: SortOrder) = error("not used")
        override fun observeDefaultTimePeriod(): Flow<TimePeriod> = error("not used")
        override suspend fun setDefaultTimePeriod(period: TimePeriod) = error("not used")
        override fun observeGridColumns(): Flow<Int> = error("not used")
        override suspend fun setGridColumns(columns: Int) = error("not used")
        override fun observeApiKey(): Flow<String?> = error("not used")
        override suspend fun setApiKey(apiKey: String?) = error("not used")
        override suspend fun getApiKey() = error("not used")
    }

    private fun createViewModel(
        model: Model = testModel(),
        isFavorite: Boolean = false,
    ): Triple<ModelDetailViewModel, FakeModelRepo, FakeFavoriteRepo> {
        val modelRepo = FakeModelRepo(model)
        val favRepo = FakeFavoriteRepo(MutableStateFlow(isFavorite))
        val browsingRepo = FakeBrowsingRepo()
        val prefsRepo = FakePrefsRepo()

        val vm = ModelDetailViewModel(
            modelId = model.id,
            getModelDetailUseCase = GetModelDetailUseCase(modelRepo),
            observeIsFavoriteUseCase = ObserveIsFavoriteUseCase(favRepo),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(favRepo),
            trackModelViewUseCase = TrackModelViewUseCase(browsingRepo),
            observeNsfwFilterUseCase = ObserveNsfwFilterUseCase(prefsRepo),
            enrichModelImagesUseCase = EnrichModelImagesUseCase(modelRepo),
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
    fun shows_error_on_load_failure() {
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
            ): PaginatedResult<Model> = error("not used")
            override suspend fun getModel(id: Long): Model = error("API error")
            override suspend fun getModelVersion(id: Long): ModelVersion = error("not used")
            override suspend fun getModelVersionByHash(hash: String): ModelVersion =
                error("not used")
        }

        val vm = ModelDetailViewModel(
            modelId = 1L,
            getModelDetailUseCase = GetModelDetailUseCase(failingRepo),
            observeIsFavoriteUseCase = ObserveIsFavoriteUseCase(FakeFavoriteRepo()),
            toggleFavoriteUseCase = ToggleFavoriteUseCase(FakeFavoriteRepo()),
            trackModelViewUseCase = TrackModelViewUseCase(FakeBrowsingRepo()),
            observeNsfwFilterUseCase = ObserveNsfwFilterUseCase(FakePrefsRepo()),
            enrichModelImagesUseCase = EnrichModelImagesUseCase(failingRepo),
        )

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("API error", state.error)
        assertNull(state.model)
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
