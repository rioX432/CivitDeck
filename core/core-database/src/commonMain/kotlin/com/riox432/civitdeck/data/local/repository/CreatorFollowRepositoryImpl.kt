package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.data.api.dto.toDomain
import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.FeedCacheDao
import com.riox432.civitdeck.data.local.dao.FollowedCreatorDao
import com.riox432.civitdeck.data.local.entity.FeedCacheEntity
import com.riox432.civitdeck.data.local.entity.FollowedCreatorEntity
import com.riox432.civitdeck.domain.model.FeedItem
import com.riox432.civitdeck.domain.model.FollowedCreator
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.repository.CreatorFollowRepository
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CreatorFollowRepositoryImpl(
    private val followedCreatorDao: FollowedCreatorDao,
    private val feedCacheDao: FeedCacheDao,
    private val api: CivitAiApi,
) : CreatorFollowRepository {

    override suspend fun followCreator(
        username: String,
        displayName: String,
        avatarUrl: String?,
    ) {
        val now = currentTimeMillis()
        followedCreatorDao.insert(
            FollowedCreatorEntity(
                username = username,
                displayName = displayName,
                avatarUrl = avatarUrl,
                followedAt = now,
                lastCheckedAt = now,
            ),
        )
    }

    override suspend fun unfollowCreator(username: String) {
        followedCreatorDao.delete(username)
        feedCacheDao.deleteByCreator(username)
    }

    override fun isFollowing(username: String): Flow<Boolean> =
        followedCreatorDao.isFollowing(username)

    override fun getFollowedCreators(): Flow<List<FollowedCreator>> =
        followedCreatorDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getFeed(forceRefresh: Boolean): List<FeedItem> {
        val creators = followedCreatorDao.getAll()
        if (creators.isEmpty()) return emptyList()

        val now = currentTimeMillis()
        val cacheThreshold = now - CACHE_TTL_MS

        if (forceRefresh) {
            refreshFeedCache(creators, now)
        } else {
            // Only refresh if cache is expired
            val cached = feedCacheDao.getAll()
            val oldestCache = cached.minOfOrNull { it.cachedAt } ?: 0L
            if (oldestCache < cacheThreshold || cached.isEmpty()) {
                refreshFeedCache(creators, now)
            }
        }

        // Determine unread threshold from the oldest lastCheckedAt among all followed creators
        val lastCheckedMin = creators.minOf { it.lastCheckedAt }

        return feedCacheDao.getAll().map { entity ->
            FeedItem(
                modelId = entity.modelId,
                creatorUsername = entity.creatorUsername,
                title = entity.title,
                thumbnailUrl = entity.thumbnailUrl,
                type = entity.type.toModelType(),
                publishedAt = entity.publishedAt,
                isUnread = entity.cachedAt > lastCheckedMin,
            )
        }
    }

    override suspend fun markFeedAsRead() {
        val now = currentTimeMillis()
        val creators = followedCreatorDao.getAll()
        creators.forEach { creator ->
            followedCreatorDao.updateLastCheckedAt(creator.username, now)
        }
    }

    override fun getUnreadCount(): Flow<Int> {
        // Use feed_cache entries added since last check as a proxy for unread count
        return followedCreatorDao.observeAll().map { creators ->
            if (creators.isEmpty()) return@map 0
            val lastCheckedMin = creators.minOf { it.lastCheckedAt }
            val allFeed = feedCacheDao.getAll()
            allFeed.count { it.cachedAt > lastCheckedMin }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun refreshFeedCache(
        creators: List<FollowedCreatorEntity>,
        now: Long,
    ) {
        // Remove expired entries
        feedCacheDao.deleteExpired(now - CACHE_TTL_MS)

        for (creator in creators) {
            try {
                val response = api.getModels(
                    username = creator.username,
                    sort = "Newest",
                    limit = FEED_PAGE_SIZE,
                )
                val entities = response.items.map { model ->
                    val domain = model.toDomain()
                    val latestVersion = domain.modelVersions.firstOrNull()
                    FeedCacheEntity(
                        modelId = domain.id,
                        creatorUsername = creator.username,
                        title = domain.name,
                        thumbnailUrl = latestVersion?.images?.firstOrNull()?.url,
                        type = domain.type.name,
                        publishedAt = latestVersion?.createdAt ?: "",
                        cachedAt = now,
                    )
                }
                feedCacheDao.insertAll(entities)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                // Skip this creator on network error, use cached data
                Logger.w(TAG, "Failed to refresh feed for ${creator.username}: ${e.message}")
            }
        }
    }

    private fun String.toModelType(): ModelType = try {
        ModelType.valueOf(this)
    } catch (_: IllegalArgumentException) {
        ModelType.Other
    }

    private fun FollowedCreatorEntity.toDomain() = FollowedCreator(
        username = username,
        displayName = displayName,
        avatarUrl = avatarUrl,
        followedAt = followedAt,
        lastCheckedAt = lastCheckedAt,
    )

    companion object {
        private const val TAG = "CreatorFollowRepositoryImpl"
        private const val CACHE_TTL_MS = 60 * 60 * 1000L // 1 hour
        private const val FEED_PAGE_SIZE = 10
    }
}
