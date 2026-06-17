package com.riox432.civitdeck.testing

import com.riox432.civitdeck.domain.model.FeedItem
import com.riox432.civitdeck.domain.model.FollowedCreator
import com.riox432.civitdeck.domain.repository.CreatorFollowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [CreatorFollowRepository] for ViewModel tests.
 */
class FakeCreatorFollowRepository(
    isFollowing: Boolean = false,
) : CreatorFollowRepository {

    val isFollowingFlow = MutableStateFlow(isFollowing)
    val followedCreatorsFlow = MutableStateFlow(emptyList<FollowedCreator>())
    val unreadCountFlow = MutableStateFlow(0)

    var followedUsername: String? = null
    var unfollowedUsername: String? = null

    override suspend fun followCreator(username: String, displayName: String, avatarUrl: String?) {
        followedUsername = username
        isFollowingFlow.value = true
    }

    override suspend fun unfollowCreator(username: String) {
        unfollowedUsername = username
        isFollowingFlow.value = false
    }

    override fun isFollowing(username: String): Flow<Boolean> = isFollowingFlow
    override fun getFollowedCreators(): Flow<List<FollowedCreator>> = followedCreatorsFlow
    override suspend fun getFeed(forceRefresh: Boolean): List<FeedItem> = emptyList()
    override suspend fun markFeedAsRead() = Unit
    override fun getUnreadCount(): Flow<Int> = unreadCountFlow
}
