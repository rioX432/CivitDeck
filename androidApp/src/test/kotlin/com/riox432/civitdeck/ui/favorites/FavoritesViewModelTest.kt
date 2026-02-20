package com.riox432.civitdeck.ui.favorites

import com.riox432.civitdeck.domain.model.FavoriteModelSummary
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import com.riox432.civitdeck.domain.usecase.ObserveFavoritesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGridColumnsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
class FavoritesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val testFavorites = listOf(
        FavoriteModelSummary(
            id = 1L, name = "Model A", type = ModelType.Checkpoint,
            nsfw = false, thumbnailUrl = null, creatorName = "artist",
            downloadCount = 100, favoriteCount = 50, rating = 4.5, favoritedAt = 1000L,
        ),
    )

    private fun createViewModel(
        favorites: List<FavoriteModelSummary> = testFavorites,
        gridColumns: Int = 2,
    ): FavoritesViewModel {
        val favRepo = object : FavoriteRepository {
            override fun observeFavorites() = flowOf(favorites)
            override fun observeIsFavorite(modelId: Long) = flowOf(false)
            override suspend fun toggleFavorite(model: com.riox432.civitdeck.domain.model.Model) =
                error("not used")
            override suspend fun addFavorite(model: com.riox432.civitdeck.domain.model.Model) =
                error("not used")
            override suspend fun removeFavorite(modelId: Long) = error("not used")
            override suspend fun getAllFavoriteIds() = error("not used")
            override suspend fun getFavoriteTypeCounts() = error("not used")
        }

        @Suppress("TooManyFunctions")
        val prefsRepo = object : UserPreferencesRepository {
            override fun observeNsfwFilterLevel(): Flow<NsfwFilterLevel> = error("not used")
            override suspend fun setNsfwFilterLevel(level: NsfwFilterLevel) = error("not used")
            override fun observeDefaultSortOrder(): Flow<SortOrder> = error("not used")
            override suspend fun setDefaultSortOrder(sort: SortOrder) = error("not used")
            override fun observeDefaultTimePeriod(): Flow<TimePeriod> = error("not used")
            override suspend fun setDefaultTimePeriod(period: TimePeriod) = error("not used")
            override fun observeGridColumns(): Flow<Int> = flowOf(gridColumns)
            override suspend fun setGridColumns(columns: Int) = error("not used")
            override fun observeApiKey(): Flow<String?> = error("not used")
            override suspend fun setApiKey(apiKey: String?) = error("not used")
            override suspend fun getApiKey() = error("not used")
        }

        return FavoritesViewModel(
            observeFavoritesUseCase = ObserveFavoritesUseCase(favRepo),
            observeGridColumnsUseCase = ObserveGridColumnsUseCase(prefsRepo),
        )
    }

    @Test
    fun favorites_flow_emits_list() = runTest(testDispatcher) {
        val vm = createViewModel()
        // WhileSubscribed requires an active subscriber to start
        val job = backgroundScope.launch(testDispatcher) { vm.favorites.collect {} }
        assertEquals(testFavorites, vm.favorites.value)
        job.cancel()
    }

    @Test
    fun gridColumns_flow_emits_value() = runTest(testDispatcher) {
        val vm = createViewModel(gridColumns = 3)
        val job = backgroundScope.launch(testDispatcher) { vm.gridColumns.collect {} }
        assertEquals(3, vm.gridColumns.value)
        job.cancel()
    }

    @Test
    fun empty_favorites_emits_empty_list() {
        val vm = createViewModel(favorites = emptyList())
        // Empty is the initial value, so no subscriber needed
        assertEquals(emptyList(), vm.favorites.value)
    }
}
