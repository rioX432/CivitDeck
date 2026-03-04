package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_search_filters")
data class SavedSearchFilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val query: String = "",
    // ModelType?.name, null means no type filter
    val selectedType: String? = null,
    // SortOrder.name
    val selectedSort: String = "MostDownloaded",
    // TimePeriod.name
    val selectedPeriod: String = "AllTime",
    // BaseModel.apiValue comma-separated, e.g. "SD 1.5,Pony"
    val selectedBaseModels: String = "",
    // NsfwFilterLevel.name
    val nsfwFilterLevel: String = "Off",
    // stored as 0/1
    val isFreshFindEnabled: Int = 0,
    // newline-separated (tags may contain commas)
    val excludedTags: String = "",
    val includedTags: String = "",
    val savedAt: Long,
)
