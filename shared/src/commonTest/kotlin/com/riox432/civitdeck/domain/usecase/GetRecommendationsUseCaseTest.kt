package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.FavoriteModelSummary
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import com.riox432.civitdeck.testModel
import com.riox432.civitdeck.testPaginatedResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetRecommendationsUseCaseTest {

    private class FakeModelRepository : ModelRepository {
        var modelsResult: PaginatedResult<Model> = testPaginatedResult()
        var lastType: ModelType? = null
        var lastTag: String? = null
        var lastSort: SortOrder? = null

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
            lastType = type
            lastTag = tag
            lastSort = sort
            return modelsResult
        }

        override suspend fun getModel(id: Long): Model = error("not used")
        override suspend fun getModelVersion(id: Long): ModelVersion = error("not used")
        override suspend fun getModelVersionByHash(hash: String): ModelVersion = error("not used")
    }

    private class FakeFavoriteRepository(
        private val favoriteIds: Set<Long> = emptySet(),
        private val typeCounts: Map<String, Int> = emptyMap(),
    ) : FavoriteRepository {
        override fun observeFavorites(): Flow<List<FavoriteModelSummary>> = error("not used")
        override fun observeIsFavorite(modelId: Long): Flow<Boolean> = error("not used")
        override suspend fun toggleFavorite(model: Model) = error("not used")
        override suspend fun addFavorite(model: Model) = error("not used")
        override suspend fun removeFavorite(modelId: Long) = error("not used")
        override suspend fun getAllFavoriteIds(): Set<Long> = favoriteIds
        override suspend fun getFavoriteTypeCounts(): Map<String, Int> = typeCounts
    }

    private class FakeBrowsingHistoryRepository(
        private val recentTypes: Map<String, Int> = emptyMap(),
        private val recentTags: Map<String, Int> = emptyMap(),
        private val recentModelIds: List<Long> = emptyList(),
    ) : BrowsingHistoryRepository {
        override suspend fun trackView(
            modelId: Long,
            modelType: String,
            creatorName: String?,
            tags: List<String>,
        ) = error("not used")

        override suspend fun getRecentTypes(limit: Int): Map<String, Int> = recentTypes
        override suspend fun getRecentCreators(limit: Int): Map<String, Int> = emptyMap()
        override suspend fun getRecentTags(limit: Int): Map<String, Int> = recentTags
        override suspend fun getRecentModelIds(limit: Int): List<Long> = recentModelIds
        override suspend fun getAllViewedModelIds(): Set<Long> = error("not used")
        override suspend fun clearAll() = error("not used")
    }

    @Suppress("TooManyFunctions")
    private class FakeUserPreferencesRepository(
        private val nsfwLevel: NsfwFilterLevel = NsfwFilterLevel.All,
    ) : UserPreferencesRepository {
        override fun observeNsfwFilterLevel(): Flow<NsfwFilterLevel> = flowOf(nsfwLevel)
        override suspend fun setNsfwFilterLevel(level: NsfwFilterLevel) = error("not used")
        override fun observeDefaultSortOrder(): Flow<SortOrder> = error("not used")
        override suspend fun setDefaultSortOrder(sort: SortOrder) = error("not used")
        override fun observeDefaultTimePeriod(): Flow<TimePeriod> = error("not used")
        override suspend fun setDefaultTimePeriod(period: TimePeriod) = error("not used")
        override fun observeGridColumns(): Flow<Int> = error("not used")
        override suspend fun setGridColumns(columns: Int) = error("not used")
        override fun observeApiKey(): Flow<String?> = error("not used")
        override suspend fun setApiKey(apiKey: String?) = error("not used")
        override suspend fun getApiKey(): String? = error("not used")
    }

    @Test
    fun returns_type_section_when_favorites_have_types() = runTest {
        val models = (1L..10L).map { testModel(id = it, type = ModelType.LORA) }
        val modelRepo = FakeModelRepository().apply {
            modelsResult = testPaginatedResult(items = models)
        }
        val favRepo = FakeFavoriteRepository(typeCounts = mapOf("LORA" to 5))
        val browsingRepo = FakeBrowsingHistoryRepository()
        val prefsRepo = FakeUserPreferencesRepository()

        val useCase = GetRecommendationsUseCase(modelRepo, favRepo, browsingRepo, prefsRepo)
        val sections = useCase()

        assertTrue(sections.isNotEmpty())
        assertEquals("Trending LORA", sections[0].title)
        assertEquals("Based on your preferences", sections[0].reason)
    }

    @Test
    fun returns_tag_section_when_browsing_has_tags() = runTest {
        val models = (1L..10L).map { testModel(id = it) }
        val modelRepo = FakeModelRepository().apply {
            modelsResult = testPaginatedResult(items = models)
        }
        val favRepo = FakeFavoriteRepository()
        val browsingRepo = FakeBrowsingHistoryRepository(recentTags = mapOf("anime" to 10))
        val prefsRepo = FakeUserPreferencesRepository()

        val useCase = GetRecommendationsUseCase(modelRepo, favRepo, browsingRepo, prefsRepo)
        val sections = useCase()

        assertTrue(sections.any { it.title.contains("anime") })
    }

    @Test
    fun returns_trending_fallback_when_no_type_or_tag() = runTest {
        val models = (1L..10L).map { testModel(id = it) }
        val modelRepo = FakeModelRepository().apply {
            modelsResult = testPaginatedResult(items = models)
        }
        val favRepo = FakeFavoriteRepository()
        val browsingRepo = FakeBrowsingHistoryRepository()
        val prefsRepo = FakeUserPreferencesRepository()

        val useCase = GetRecommendationsUseCase(modelRepo, favRepo, browsingRepo, prefsRepo)
        val sections = useCase()

        assertEquals(1, sections.size)
        assertEquals("Trending This Week", sections[0].title)
        assertEquals("Popular models", sections[0].reason)
    }

    @Test
    fun excludes_favorite_and_viewed_models() = runTest {
        val models = (1L..12L).map { testModel(id = it) }
        val modelRepo = FakeModelRepository().apply {
            modelsResult = testPaginatedResult(items = models)
        }
        val favRepo = FakeFavoriteRepository(favoriteIds = setOf(1L, 2L))
        val browsingRepo = FakeBrowsingHistoryRepository(recentModelIds = listOf(3L, 4L))
        val prefsRepo = FakeUserPreferencesRepository()

        val useCase = GetRecommendationsUseCase(modelRepo, favRepo, browsingRepo, prefsRepo)
        val sections = useCase()

        assertTrue(sections.isNotEmpty())
        val allModelIds = sections.flatMap { it.models.map { m -> m.id } }
        assertTrue(1L !in allModelIds)
        assertTrue(2L !in allModelIds)
        assertTrue(3L !in allModelIds)
        assertTrue(4L !in allModelIds)
    }

    @Test
    fun returns_empty_when_all_models_filtered() = runTest {
        val modelRepo = FakeModelRepository().apply {
            modelsResult = testPaginatedResult(items = emptyList())
        }
        val favRepo = FakeFavoriteRepository()
        val browsingRepo = FakeBrowsingHistoryRepository()
        val prefsRepo = FakeUserPreferencesRepository()

        val useCase = GetRecommendationsUseCase(modelRepo, favRepo, browsingRepo, prefsRepo)
        val sections = useCase()

        assertTrue(sections.isEmpty())
    }

    @Test
    fun favorite_types_weighted_higher_than_browsing_types() = runTest {
        val loraModels = (1L..10L).map { testModel(id = it, type = ModelType.LORA) }
        val modelRepo = FakeModelRepository().apply {
            modelsResult = testPaginatedResult(items = loraModels)
        }
        // LORA: 2*3=6 from favorites, Checkpoint: 5 from browsing
        // LORA should win because favorite weight is 3x
        val favRepo = FakeFavoriteRepository(typeCounts = mapOf("LORA" to 2))
        val browsingRepo = FakeBrowsingHistoryRepository(
            recentTypes = mapOf("Checkpoint" to 5),
        )
        val prefsRepo = FakeUserPreferencesRepository()

        val useCase = GetRecommendationsUseCase(modelRepo, favRepo, browsingRepo, prefsRepo)
        val sections = useCase()

        assertTrue(sections.isNotEmpty())
        assertEquals(ModelType.LORA, modelRepo.lastType)
    }

    @Test
    fun section_limited_to_ten_models() = runTest {
        val models = (1L..20L).map { testModel(id = it) }
        val modelRepo = FakeModelRepository().apply {
            modelsResult = testPaginatedResult(items = models)
        }
        val favRepo = FakeFavoriteRepository()
        val browsingRepo = FakeBrowsingHistoryRepository()
        val prefsRepo = FakeUserPreferencesRepository()

        val useCase = GetRecommendationsUseCase(modelRepo, favRepo, browsingRepo, prefsRepo)
        val sections = useCase()

        assertTrue(sections.isNotEmpty())
        assertTrue(sections[0].models.size <= 10)
    }
}
