package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.FeedItem
import com.riox432.civitdeck.domain.model.FollowedCreator
import kotlinx.coroutines.flow.Flow

interface CreatorFollowRepository {
    suspend fun followCreator(username: String, displayName: String, avatarUrl: String?)
    suspend fun unfollowCreator(username: String)
    fun isFollowing(username: String): Flow<Boolean>
    fun getFollowedCreators(): Flow<List<FollowedCreator>>
    suspend fun getFeed(forceRefresh: Boolean): List<FeedItem>
    suspend fun markFeedAsRead()
    fun getUnreadCount(): Flow<Int>
}
