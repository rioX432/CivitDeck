package com.riox432.civitdeck.domain.model

data class FollowedCreator(
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val followedAt: Long,
    val lastCheckedAt: Long,
)
