package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val nsfwFilterLevel: String = "Off",
    val defaultSortOrder: String = "MostDownloaded",
    val defaultTimePeriod: String = "AllTime",
    val gridColumns: Int = 2,
    val apiKey: String? = null,
)
