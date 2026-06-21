package com.riox432.civitdeck.feature.search.presentation

import com.riox432.civitdeck.domain.model.HiddenModel
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.SavedSearchFilter
import com.riox432.civitdeck.domain.repository.ExcludedTagRepository
import com.riox432.civitdeck.domain.repository.HiddenModelRepository
import com.riox432.civitdeck.domain.repository.HuggingFaceRepository
import com.riox432.civitdeck.domain.repository.ModelFileHashRepository
import com.riox432.civitdeck.domain.repository.SavedSearchFilterRepository
import com.riox432.civitdeck.domain.repository.SearchHistoryRepository
import com.riox432.civitdeck.domain.repository.TensorArtRepository
import com.riox432.civitdeck.domain.usecase.AddExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.GetExcludedTagsUseCase
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.ObserveFavoritesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObserveOwnedModelHashesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveQualityThresholdUseCase
import com.riox432.civitdeck.domain.usecase.RemoveExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.AddSearchHistoryUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.DeleteSavedSearchFilterUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.DeleteSearchHistoryItemUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetHiddenModelIdsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetRecommendationsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.HideModelUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.MultiSourceSearchUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.ObserveSavedSearchFiltersUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.ObserveSearchHistoryUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.SaveSearchFilterUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.TrackRecommendationClickUseCase
import com.riox432.civitdeck.testing.FakeAppBehaviorPreferencesRepository
import com.riox432.civitdeck.testing.FakeBrowsingHistoryRepository
import com.riox432.civitdeck.testing.FakeContentFilterPreferencesRepository
import com.riox432.civitdeck.testing.FakeFavoriteRepository
import com.riox432.civitdeck.testing.FakeModelRepository
import com.riox432.civitdeck.testing.testModel
import com.riox432.civitdeck.testing.testPaginatedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [ModelSearchViewModel.observeNsfwFilter]: a *changed* NSFW filter level
 * (after the initial emission) must trigger both `refresh()` (a new page load)
 * and `loadRecommendations()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ModelSearchViewModelTest {

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class NoOpHuggingFaceRepo : HuggingFaceRepository {
        override suspend fun searchModels(query: String?, limit: Int, offset: Int) = emptyList<Model>()
    }

    private class NoOpTensorArtRepo : TensorArtRepository {
        override suspend fun searchModels(query: String, page: Int, pageSize: Int) = emptyList<Model>()
    }

    private class FakeExcludedTagRepo : ExcludedTagRepository {
        override suspend fun getExcludedTags() = emptyList<String>()
        override suspend fun addExcludedTag(tag: String) = Unit
        override suspend fun removeExcludedTag(tag: String) = Unit
    }

    private class FakeHiddenModelRepo : HiddenModelRepository {
        override suspend fun getHiddenModelIds() = emptySet<Long>()
        override suspend fun getHiddenModels() = emptyList<HiddenModel>()
        override suspend fun hideModel(modelId: Long, modelName: String) = Unit
        override suspend fun unhideModel(modelId: Long) = Unit
    }

    private class FakeSavedSearchFilterRepo : SavedSearchFilterRepository {
        override fun observeAll(): Flow<List<SavedSearchFilter>> = MutableStateFlow(emptyList())
        override suspend fun save(filter: SavedSearchFilter) = 1L
        override suspend fun delete(id: Long) = Unit
    }

    private class FakeSearchHistoryRepo : SearchHistoryRepository {
        override fun observeRecentSearches(): Flow<List<String>> = MutableStateFlow(emptyList())
        override suspend fun addSearch(query: String) = Unit
        override suspend fun deleteSearch(query: String) = Unit
        override suspend fun clearAll() = Unit
    }

    private class FakeModelFileHashRepo : ModelFileHashRepository {
        override suspend fun verifyFileHash(fileId: Long, sha256Hash: String) = Unit
        override fun observeOwnedHashes(): Flow<Set<String>> = MutableStateFlow(emptySet())
        override suspend fun getOwnedHashes() = emptySet<String>()
        override fun observeFileCount(): Flow<Int> = MutableStateFlow(0)
        override fun observeMatchedCount(): Flow<Int> = MutableStateFlow(0)
        override fun observeUpdatesAvailableCount(): Flow<Int> = MutableStateFlow(0)
    }

    private class FakeDisplayPrefsRepo : com.riox432.civitdeck.domain.repository.DisplayPreferencesRepository {
        override fun observeDefaultSortOrder() =
            MutableStateFlow(com.riox432.civitdeck.domain.model.SortOrder.MostDownloaded)
        override suspend fun setDefaultSortOrder(sort: com.riox432.civitdeck.domain.model.SortOrder) = Unit
        override fun observeDefaultTimePeriod() =
            MutableStateFlow(com.riox432.civitdeck.domain.model.TimePeriod.AllTime)
        override suspend fun setDefaultTimePeriod(period: com.riox432.civitdeck.domain.model.TimePeriod) = Unit
        override fun observeGridColumns(): Flow<Int> = MutableStateFlow(2)
        override suspend fun setGridColumns(columns: Int) = Unit
        override fun observeAccentColor() =
            MutableStateFlow(com.riox432.civitdeck.domain.model.AccentColor.Blue)
        override suspend fun setAccentColor(color: com.riox432.civitdeck.domain.model.AccentColor) = Unit
        override fun observeAmoledDarkMode(): Flow<Boolean> = MutableStateFlow(false)
        override suspend fun setAmoledDarkMode(enabled: Boolean) = Unit
        override fun observeThemeMode() =
            MutableStateFlow(com.riox432.civitdeck.domain.model.ThemeMode.SYSTEM)
        override suspend fun setThemeMode(mode: com.riox432.civitdeck.domain.model.ThemeMode) = Unit
    }

    private class TestDeps(
        val vm: ModelSearchViewModel,
        val modelRepo: FakeModelRepository,
        val favRepo: FakeFavoriteRepository,
        val nsfwPrefs: FakeContentFilterPreferencesRepository,
    )

    @Suppress("LongMethod")
    private fun TestScope.createViewModel(): TestDeps {
        // StandardTestDispatcher defers the ViewModel's init coroutines until the
        // scheduler is advanced — this avoids the eager-construction NPE where
        // init-launched work references properties declared after `init`.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val modelRepo = FakeModelRepository(
            pages = listOf(testPaginatedResult(items = listOf(testModel(id = 1L)))),
        )
        val favRepo = FakeFavoriteRepository()
        val browsingRepo = FakeBrowsingHistoryRepository()
        val nsfwPrefs = FakeContentFilterPreferencesRepository(NsfwFilterLevel.Off)
        val appBehavior = FakeAppBehaviorPreferencesRepository()
        val hfRepo = NoOpHuggingFaceRepo()
        val taRepo = NoOpTensorArtRepo()
        val excludedTagRepo = FakeExcludedTagRepo()
        val hiddenRepo = FakeHiddenModelRepo()
        val savedFilterRepo = FakeSavedSearchFilterRepo()
        val searchHistoryRepo = FakeSearchHistoryRepo()
        val hashRepo = FakeModelFileHashRepo()
        val displayRepo = FakeDisplayPrefsRepo()

        val multiSource = MultiSourceSearchUseCase(modelRepo, hfRepo, taRepo)

        val vm = ModelSearchViewModel(
            coreUseCases = SearchCoreUseCases(
                getModels = GetModelsUseCase(modelRepo),
                multiSourceSearch = multiSource,
                getRecommendations = GetRecommendationsUseCase(
                    modelRepository = modelRepo,
                    favoriteRepository = favRepo,
                    browsingHistoryRepository = browsingRepo,
                    userPreferencesRepository = nsfwPrefs,
                    appBehaviorRepository = appBehavior,
                ),
                getViewedModelIds = GetViewedModelIdsUseCase(browsingRepo),
                trackRecommendationClick = TrackRecommendationClickUseCase(browsingRepo),
            ),
            historyUseCases = SearchHistoryUseCases(
                observeSearchHistory = ObserveSearchHistoryUseCase(searchHistoryRepo),
                addSearchHistory = AddSearchHistoryUseCase(searchHistoryRepo),
                deleteSearchHistoryItem = DeleteSearchHistoryItemUseCase(searchHistoryRepo),
                clearSearchHistory = ClearSearchHistoryUseCase(searchHistoryRepo),
            ),
            filterUseCases = SearchFilterUseCases(
                getExcludedTags = GetExcludedTagsUseCase(excludedTagRepo),
                addExcludedTag = AddExcludedTagUseCase(excludedTagRepo),
                removeExcludedTag = RemoveExcludedTagUseCase(excludedTagRepo),
                getHiddenModelIds = GetHiddenModelIdsUseCase(hiddenRepo),
                hideModel = HideModelUseCase(hiddenRepo),
                observeSavedSearchFilters = ObserveSavedSearchFiltersUseCase(savedFilterRepo),
                saveSearchFilter = SaveSearchFilterUseCase(savedFilterRepo),
                deleteSavedSearchFilter = DeleteSavedSearchFilterUseCase(savedFilterRepo),
            ),
            preferencesUseCases = SearchPreferencesUseCases(
                observeNsfwFilter = ObserveNsfwFilterUseCase(nsfwPrefs),
                observeGridColumns = ObserveGridColumnsUseCase(displayRepo),
                observeDefaultSortOrder = ObserveDefaultSortOrderUseCase(displayRepo),
                observeDefaultTimePeriod = ObserveDefaultTimePeriodUseCase(displayRepo),
                observeQualityThreshold = ObserveQualityThresholdUseCase(appBehavior),
            ),
            favoritesUseCases = SearchFavoritesUseCases(
                toggleFavorite = ToggleFavoriteUseCase(favRepo),
                observeFavorites = ObserveFavoritesUseCase(favRepo),
                observeOwnedModelHashes = ObserveOwnedModelHashesUseCase(hashRepo),
            ),
        )
        return TestDeps(vm, modelRepo, favRepo, nsfwPrefs)
    }

    @Test
    fun init_loads_first_page_and_default_filter() = runTest {
        val deps = createViewModel()
        advanceUntilIdle()
        // loadDefaults() -> loadFirst() performs an initial CivitAI page load.
        assertTrue(deps.modelRepo.getModelsCallCount >= 1)
        assertEquals(NsfwFilterLevel.Off, deps.vm.uiState.value.nsfwFilterLevel)
    }

    @Test
    fun nsfw_filter_change_triggers_refresh_and_recommendations_reload() = runTest {
        val deps = createViewModel()
        advanceUntilIdle()
        val modelCallsAfterInit = deps.modelRepo.getModelsCallCount
        // getAllFavoriteIds is called once per loadRecommendations() pass.
        val recommendationCallsAfterInit = deps.favRepo.getAllFavoriteIdsCount

        // Emit a *changed* level: should reload recommendations AND refresh the page.
        deps.nsfwPrefs.nsfwFilterLevelFlow.value = NsfwFilterLevel.All
        advanceUntilIdle()

        assertEquals(NsfwFilterLevel.All, deps.vm.uiState.value.nsfwFilterLevel)
        assertTrue(
            deps.modelRepo.getModelsCallCount > modelCallsAfterInit,
            "refresh() should have triggered an additional page load",
        )
        assertTrue(
            deps.favRepo.getAllFavoriteIdsCount > recommendationCallsAfterInit,
            "loadRecommendations() should have run again",
        )
    }

    @Test
    fun same_nsfw_level_does_not_refresh() = runTest {
        val deps = createViewModel()
        advanceUntilIdle()
        val modelCallsAfterInit = deps.modelRepo.getModelsCallCount

        // Re-emitting the same value must NOT trigger a refresh.
        deps.nsfwPrefs.nsfwFilterLevelFlow.value = NsfwFilterLevel.Off
        advanceUntilIdle()

        assertEquals(modelCallsAfterInit, deps.modelRepo.getModelsCallCount)
    }
}
