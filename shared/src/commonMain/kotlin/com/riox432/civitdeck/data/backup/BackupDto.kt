package com.riox432.civitdeck.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupDto(
    val metadata: BackupMetadataDto,
    val collections: List<CollectionDto>? = null,
    val collectionModels: List<CollectionModelDto>? = null,
    val modelNotes: List<ModelNoteDto>? = null,
    val personalTags: List<PersonalTagDto>? = null,
    val savedPrompts: List<SavedPromptDto>? = null,
    val userPreferences: UserPreferencesDto? = null,
    val savedSearchFilters: List<SavedSearchFilterDto>? = null,
    val followedCreators: List<FollowedCreatorDto>? = null,
    val comfyUIConnections: List<ComfyUIConnectionDto>? = null,
    val sdWebUIConnections: List<SDWebUIConnectionDto>? = null,
    val externalServerConfigs: List<ExternalServerConfigDto>? = null,
    val hiddenModels: List<HiddenModelDto>? = null,
    val excludedTags: List<ExcludedTagDto>? = null,
)

@Serializable
data class BackupMetadataDto(
    val version: Int = 1,
    val createdAt: Long,
    val appVersion: String = "1.0.0",
    val categories: List<String>,
)

@Serializable
data class CollectionDto(
    val id: Long,
    val name: String,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class CollectionModelDto(
    val collectionId: Long,
    val modelId: Long,
    val name: String,
    val type: String,
    val nsfw: Boolean,
    val thumbnailUrl: String? = null,
    val creatorName: String? = null,
    val downloadCount: Int,
    val favoriteCount: Int,
    val rating: Double,
    val addedAt: Long,
)

@Serializable
data class ModelNoteDto(
    val modelId: Long,
    val noteText: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class PersonalTagDto(
    val modelId: Long,
    val tag: String,
    val addedAt: Long,
)

@Serializable
data class SavedPromptDto(
    val prompt: String,
    val negativePrompt: String? = null,
    val sampler: String? = null,
    val steps: Int? = null,
    val cfgScale: Double? = null,
    val seed: Long? = null,
    val modelName: String? = null,
    val size: String? = null,
    val sourceImageUrl: String? = null,
    val savedAt: Long,
    val isTemplate: Boolean = false,
    val templateName: String? = null,
    val autoSaved: Boolean = false,
    val templateVariables: String? = null,
    val templateType: String? = null,
)

@Serializable
data class UserPreferencesDto(
    val nsfwFilterLevel: String = "Off",
    val defaultSortOrder: String = "MostDownloaded",
    val defaultTimePeriod: String = "AllTime",
    val gridColumns: Int = 2,
    val powerUserMode: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val pollingIntervalMinutes: Int = 0,
    val nsfwBlurSoft: Int = 75,
    val nsfwBlurMature: Int = 25,
    val nsfwBlurExplicit: Int = 0,
    val offlineCacheEnabled: Boolean = true,
    val cacheSizeLimitMb: Int = 200,
    val accentColor: String = "Blue",
    val amoledDarkMode: Boolean = false,
    val themeMode: String = "SYSTEM",
    val customNavShortcuts: String = "",
)

@Serializable
data class SavedSearchFilterDto(
    val name: String,
    val query: String = "",
    val selectedType: String? = null,
    val selectedSort: String = "MostDownloaded",
    val selectedPeriod: String = "AllTime",
    val selectedBaseModels: String = "",
    val nsfwFilterLevel: String = "Off",
    val isFreshFindEnabled: Int = 0,
    val excludedTags: String = "",
    val includedTags: String = "",
    val selectedSources: String = "CIVITAI",
    val savedAt: Long,
)

@Serializable
data class FollowedCreatorDto(
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val followedAt: Long,
    val lastCheckedAt: Long,
)

@Serializable
data class ComfyUIConnectionDto(
    val name: String,
    val hostname: String,
    val port: Int,
    val isActive: Boolean,
    val createdAt: Long,
)

@Serializable
data class SDWebUIConnectionDto(
    val name: String,
    val hostname: String,
    val port: Int,
    val isActive: Boolean,
    val createdAt: Long,
)

@Serializable
data class ExternalServerConfigDto(
    val name: String,
    val baseUrl: String,
    val apiKey: String = "",
    val isActive: Boolean,
    val createdAt: Long,
)

@Serializable
data class HiddenModelDto(
    val modelId: Long,
    val modelName: String,
    val hiddenAt: Long,
)

@Serializable
data class ExcludedTagDto(
    val tag: String,
    val addedAt: Long,
)
