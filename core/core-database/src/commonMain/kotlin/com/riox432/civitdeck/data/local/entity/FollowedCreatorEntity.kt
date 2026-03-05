package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "followed_creators")
data class FollowedCreatorEntity(
    @PrimaryKey val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val followedAt: Long,
    val lastCheckedAt: Long,
)
