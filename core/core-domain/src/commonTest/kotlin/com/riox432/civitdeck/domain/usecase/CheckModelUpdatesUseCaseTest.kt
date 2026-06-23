package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.FeedItem
import com.riox432.civitdeck.domain.model.FollowedCreator
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.model.ModelLicenseInfo
import com.riox432.civitdeck.domain.model.ModelSearchQuery
import com.riox432.civitdeck.domain.model.ModelStats
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.UpdateSource
import com.riox432.civitdeck.domain.repository.CreatorFollowRepository
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.repository.ModelVersionCheckpointRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CheckModelUpdatesUseCaseTest {

    @Test
    fun reports_update_when_latest_version_differs_from_checkpoint() = runTest {
        // Arrange: model 1 is favorited; checkpoint records version 100 but the model now
        // has version 200 -> should be reported as a FAVORITE update.
        val favorites = FakeFavoriteRepository(ids = setOf(1L))
        val models = FakeModelRepo(mapOf(1L to model(id = 1L, versionId = 200L)))
        val checkpoints = FakeCheckpointRepo(mutableMapOf(1L to (100L to 0L)))
        val useCase = CheckModelUpdatesUseCase(favorites, models, checkpoints, NoFollows())

        val updates = useCase()

        assertEquals(1, updates.size)
        assertEquals(1L, updates[0].modelId)
        assertEquals(200L, updates[0].newVersionId)
        assertEquals(UpdateSource.FAVORITE, updates[0].source)
    }

    @Test
    fun does_not_report_when_version_matches_checkpoint() = runTest {
        val favorites = FakeFavoriteRepository(ids = setOf(1L))
        val models = FakeModelRepo(mapOf(1L to model(id = 1L, versionId = 100L)))
        val checkpoints = FakeCheckpointRepo(mutableMapOf(1L to (100L to 0L)))
        val useCase = CheckModelUpdatesUseCase(favorites, models, checkpoints, NoFollows())

        val updates = useCase()

        assertTrue(updates.isEmpty())
    }

    @Test
    fun first_seen_model_records_checkpoint_without_reporting_update() = runTest {
        // No prior checkpoint -> the use case stores the current version but reports nothing.
        val favorites = FakeFavoriteRepository(ids = setOf(7L))
        val models = FakeModelRepo(mapOf(7L to model(id = 7L, versionId = 55L)))
        val checkpoints = FakeCheckpointRepo()
        val useCase = CheckModelUpdatesUseCase(favorites, models, checkpoints, NoFollows())

        val updates = useCase()

        assertTrue(updates.isEmpty())
        assertEquals(55L, checkpoints.saved[7L])
    }

    @Test
    fun continues_when_one_model_fetch_throws() = runTest {
        // Model 2 fails to fetch; the use case must skip it and still report model 1.
        val favorites = FakeFavoriteRepository(ids = setOf(1L, 2L))
        val models = FakeModelRepo(
            available = mapOf(1L to model(id = 1L, versionId = 200L)),
            failing = setOf(2L),
        )
        val checkpoints = FakeCheckpointRepo(
            mutableMapOf(1L to (100L to 0L), 2L to (300L to 0L)),
        )
        val useCase = CheckModelUpdatesUseCase(favorites, models, checkpoints, NoFollows())

        val updates = useCase()

        assertEquals(listOf(1L), updates.map { it.modelId })
    }

    @Test
    fun returns_empty_when_no_favorites_and_no_follows() = runTest {
        val useCase = CheckModelUpdatesUseCase(
            FakeFavoriteRepository(ids = emptySet()),
            FakeModelRepo(emptyMap()),
            FakeCheckpointRepo(),
            NoFollows(),
        )

        assertTrue(useCase().isEmpty())
    }

    @Test
    fun deletes_stale_checkpoints_for_favorites() = runTest {
        // Checkpoint exists for model 9 which is no longer favorited -> should be pruned.
        val favorites = FakeFavoriteRepository(ids = setOf(1L))
        val models = FakeModelRepo(mapOf(1L to model(id = 1L, versionId = 100L)))
        val checkpoints = FakeCheckpointRepo(mutableMapOf(1L to (100L to 0L), 9L to (10L to 0L)))
        val useCase = CheckModelUpdatesUseCase(favorites, models, checkpoints, NoFollows())

        useCase()

        assertEquals(setOf(1L), checkpoints.lastStaleKeepIds)
    }

    @Test
    fun reports_followed_creator_models_excluding_favorites() = runTest {
        // Feed has models 1 and 2; model 1 is also a favorite and must not be double-counted
        // as a FOLLOWED update.
        val favorites = FakeFavoriteRepository(ids = setOf(1L))
        val models = FakeModelRepo(
            mapOf(
                1L to model(id = 1L, versionId = 100L),
                2L to model(id = 2L, versionId = 222L),
            ),
        )
        val checkpoints = FakeCheckpointRepo(
            mutableMapOf(1L to (100L to 0L), 2L to (111L to 0L)),
        )
        val follows = FakeCreatorFollowRepo(
            creators = listOf(creator("alice")),
            feed = listOf(feedItem(1L), feedItem(2L)),
        )
        val useCase = CheckModelUpdatesUseCase(favorites, models, checkpoints, follows)

        val updates = useCase()

        // Only model 2 reported (as FOLLOWED); model 1 unchanged + excluded from follow set.
        assertEquals(listOf(2L), updates.map { it.modelId })
        assertEquals(UpdateSource.FOLLOWED, updates.single().source)
    }

    // --- Fixtures & fakes ---

    private fun model(id: Long, versionId: Long) = Model(
        id = id,
        name = "Model $id",
        description = null,
        type = ModelType.Checkpoint,
        nsfw = false,
        tags = emptyList(),
        mode = null,
        creator = null,
        stats = ModelStats(0, 0, 0, 0, 0.0),
        modelVersions = listOf(
            ModelVersion(
                id = versionId,
                modelId = id,
                name = "v$versionId",
                description = null,
                createdAt = "",
                baseModel = null,
                trainedWords = emptyList(),
                downloadUrl = "",
                files = emptyList(),
                images = emptyList<ModelImage>(),
                stats = null,
            ),
        ),
    )

    private fun feedItem(modelId: Long) = FeedItem(
        modelId = modelId,
        creatorUsername = "alice",
        title = "Item $modelId",
        thumbnailUrl = null,
        type = ModelType.Checkpoint,
        publishedAt = "",
        isUnread = true,
    )

    private fun creator(username: String) = FollowedCreator(
        username = username,
        displayName = username,
        avatarUrl = null,
        followedAt = 0L,
        lastCheckedAt = 0L,
    )

    private class FakeFavoriteRepository(private val ids: Set<Long>) : FavoriteRepository {
        override fun observeFavorites() = throw NotImplementedError()
        override fun observeIsFavorite(modelId: Long) = throw NotImplementedError()
        override suspend fun toggleFavorite(model: Model) = throw NotImplementedError()
        override suspend fun addFavorite(model: Model) = throw NotImplementedError()
        override suspend fun removeFavorite(modelId: Long) = throw NotImplementedError()
        override suspend fun getAllFavoriteIds(): Set<Long> = ids
        override suspend fun getFavoriteTypeCounts(): Map<String, Int> = emptyMap()
    }

    private class FakeModelRepo(
        private val available: Map<Long, Model>,
        private val failing: Set<Long> = emptySet(),
    ) : ModelRepository {
        override suspend fun getModels(query: ModelSearchQuery): PaginatedResult<Model> =
            throw NotImplementedError()
        override suspend fun getModel(id: Long): Model {
            if (id in failing) error("fetch failed for $id")
            return available.getValue(id)
        }
        override suspend fun getModelVersion(id: Long): ModelVersion = throw NotImplementedError()
        override suspend fun getModelVersionByHash(hash: String): ModelVersion =
            throw NotImplementedError()
        override suspend fun getModelLicense(versionId: Long): ModelLicenseInfo? =
            throw NotImplementedError()
    }

    private class FakeCheckpointRepo(
        private val initial: MutableMap<Long, Pair<Long, Long>> = mutableMapOf(),
    ) : ModelVersionCheckpointRepository {
        val saved = mutableMapOf<Long, Long>()
        var lastStaleKeepIds: Set<Long>? = null
        override suspend fun getCheckpoint(modelId: Long): Long? = initial[modelId]?.first
        override suspend fun getAllCheckpoints(): Map<Long, Pair<Long, Long>> = initial
        override suspend fun saveCheckpoint(modelId: Long, versionId: Long) {
            saved[modelId] = versionId
        }
        override suspend fun saveCheckpoints(checkpoints: Map<Long, Long>) {
            saved.putAll(checkpoints)
        }
        override suspend fun deleteStaleCheckpoints(activeModelIds: Set<Long>) {
            lastStaleKeepIds = activeModelIds
        }
    }

    private class NoFollows : CreatorFollowRepository {
        override suspend fun followCreator(username: String, displayName: String, avatarUrl: String?) =
            throw NotImplementedError()
        override suspend fun unfollowCreator(username: String) = throw NotImplementedError()
        override fun isFollowing(username: String): Flow<Boolean> = throw NotImplementedError()
        override fun getFollowedCreators(): Flow<List<FollowedCreator>> = flowOf(emptyList())
        override suspend fun getFeed(forceRefresh: Boolean): List<FeedItem> = emptyList()
        override suspend fun markFeedAsRead() = throw NotImplementedError()
        override fun getUnreadCount(): Flow<Int> = throw NotImplementedError()
    }

    private class FakeCreatorFollowRepo(
        private val creators: List<FollowedCreator>,
        private val feed: List<FeedItem>,
    ) : CreatorFollowRepository {
        override suspend fun followCreator(username: String, displayName: String, avatarUrl: String?) =
            throw NotImplementedError()
        override suspend fun unfollowCreator(username: String) = throw NotImplementedError()
        override fun isFollowing(username: String): Flow<Boolean> = throw NotImplementedError()
        override fun getFollowedCreators(): Flow<List<FollowedCreator>> = flowOf(creators)
        override suspend fun getFeed(forceRefresh: Boolean): List<FeedItem> = feed
        override suspend fun markFeedAsRead() = throw NotImplementedError()
        override fun getUnreadCount(): Flow<Int> = throw NotImplementedError()
    }
}
