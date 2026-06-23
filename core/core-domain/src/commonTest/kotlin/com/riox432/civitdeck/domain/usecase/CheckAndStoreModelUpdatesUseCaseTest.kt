package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.FeedItem
import com.riox432.civitdeck.domain.model.FollowedCreator
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.model.ModelLicenseInfo
import com.riox432.civitdeck.domain.model.ModelSearchQuery
import com.riox432.civitdeck.domain.model.ModelStats
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.ModelUpdate
import com.riox432.civitdeck.domain.model.ModelUpdateNotification
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.UpdateSource
import com.riox432.civitdeck.domain.repository.CreatorFollowRepository
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.repository.ModelUpdateNotificationRepository
import com.riox432.civitdeck.domain.repository.ModelVersionCheckpointRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [CheckAndStoreModelUpdatesUseCase]: it splits detected updates by
 * [UpdateSource], persists each group, and triggers cleanup only when there is
 * something to store.
 */
class CheckAndStoreModelUpdatesUseCaseTest {

    @Test
    fun stores_favorite_and_followed_updates_separately_then_cleans_up() = runTest {
        // Arrange: model 1 is a favorite with a newer version; model 2 comes from a
        // followed creator's feed (and is not a favorite) and also has a newer version.
        val favorites = FakeFavoriteRepository(ids = setOf(1L))
        val models = FakeModelRepo(
            mapOf(
                1L to model(id = 1L, versionId = 200L),
                2L to model(id = 2L, versionId = 400L),
            ),
        )
        val checkpoints = FakeCheckpointRepo(
            mutableMapOf(1L to (100L to 0L), 2L to (300L to 0L)),
        )
        val follows = FakeCreatorFollowRepo(
            creators = listOf(creator("alice")),
            feed = listOf(feedItem(2L)),
        )
        val inner = CheckModelUpdatesUseCase(favorites, models, checkpoints, follows)
        val notifications = FakeNotificationRepo()
        val useCase = CheckAndStoreModelUpdatesUseCase(inner, notifications)

        // Act
        val result = useCase()

        // Assert: both updates returned, each saved under its own source, cleanup ran once.
        assertEquals(setOf(1L, 2L), result.map { it.modelId }.toSet())
        assertEquals(listOf(1L), notifications.savedBySource[UpdateSource.FAVORITE]?.map { it.modelId })
        assertEquals(listOf(2L), notifications.savedBySource[UpdateSource.FOLLOWED]?.map { it.modelId })
        assertEquals(1, notifications.cleanupCount)
    }

    @Test
    fun does_not_save_or_clean_up_when_there_are_no_updates() = runTest {
        // Arrange: the single favorite is already at its checkpoint version -> no updates.
        val favorites = FakeFavoriteRepository(ids = setOf(1L))
        val models = FakeModelRepo(mapOf(1L to model(id = 1L, versionId = 100L)))
        val checkpoints = FakeCheckpointRepo(mutableMapOf(1L to (100L to 0L)))
        val inner = CheckModelUpdatesUseCase(favorites, models, checkpoints, NoFollows())
        val notifications = FakeNotificationRepo()
        val useCase = CheckAndStoreModelUpdatesUseCase(inner, notifications)

        // Act
        val result = useCase()

        // Assert: empty result, nothing persisted, no cleanup.
        assertTrue(result.isEmpty())
        assertTrue(notifications.savedBySource.isEmpty())
        assertEquals(0, notifications.cleanupCount)
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

    private class FakeModelRepo(private val available: Map<Long, Model>) : ModelRepository {
        override suspend fun getModels(query: ModelSearchQuery): PaginatedResult<Model> =
            throw NotImplementedError()
        override suspend fun getModel(id: Long): Model = available.getValue(id)
        override suspend fun getModelVersion(id: Long): ModelVersion = throw NotImplementedError()
        override suspend fun getModelVersionByHash(hash: String): ModelVersion =
            throw NotImplementedError()
        override suspend fun getModelLicense(versionId: Long): ModelLicenseInfo? =
            throw NotImplementedError()
    }

    private class FakeCheckpointRepo(
        private val initial: MutableMap<Long, Pair<Long, Long>> = mutableMapOf(),
    ) : ModelVersionCheckpointRepository {
        override suspend fun getCheckpoint(modelId: Long): Long? = initial[modelId]?.first
        override suspend fun getAllCheckpoints(): Map<Long, Pair<Long, Long>> = initial
        override suspend fun saveCheckpoint(modelId: Long, versionId: Long) = Unit
        override suspend fun saveCheckpoints(checkpoints: Map<Long, Long>) = Unit
        override suspend fun deleteStaleCheckpoints(activeModelIds: Set<Long>) = Unit
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

    private class FakeNotificationRepo : ModelUpdateNotificationRepository {
        val savedBySource = mutableMapOf<UpdateSource, List<ModelUpdate>>()
        var cleanupCount = 0
        override fun observeNotifications(): Flow<List<ModelUpdateNotification>> = flowOf(emptyList())
        override fun observeUnreadCount(): Flow<Int> = flowOf(0)
        override suspend fun saveNotifications(updates: List<ModelUpdate>, source: UpdateSource) {
            savedBySource[source] = updates
        }
        override suspend fun markRead(notificationId: Long) = Unit
        override suspend fun markAllRead() = Unit
        override suspend fun cleanupOldNotifications() {
            cleanupCount++
        }
    }
}
