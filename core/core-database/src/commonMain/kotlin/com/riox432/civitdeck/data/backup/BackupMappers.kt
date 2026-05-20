package com.riox432.civitdeck.data.backup

import com.riox432.civitdeck.data.local.entity.CollectionEntity
import com.riox432.civitdeck.data.local.entity.CollectionModelEntity
import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.data.local.entity.ExcludedTagEntity
import com.riox432.civitdeck.data.local.entity.ExternalServerConfigEntity
import com.riox432.civitdeck.data.local.entity.FollowedCreatorEntity
import com.riox432.civitdeck.data.local.entity.HiddenModelEntity
import com.riox432.civitdeck.data.local.entity.ModelNoteEntity
import com.riox432.civitdeck.data.local.entity.PersonalTagEntity
import com.riox432.civitdeck.data.local.entity.SDWebUIConnectionEntity
import com.riox432.civitdeck.data.local.entity.SavedPromptEntity
import com.riox432.civitdeck.data.local.entity.SavedSearchFilterEntity
import com.riox432.civitdeck.data.local.entity.UserPreferencesEntity

// --- Entity -> DTO mappers ---

internal fun CollectionEntity.toDto() = CollectionDto(id, name, isDefault, createdAt, updatedAt)

internal fun CollectionModelEntity.toDto() = CollectionModelDto(
    collectionId, modelId, name, type, nsfw, thumbnailUrl,
    creatorName, downloadCount, favoriteCount, rating, addedAt,
)

internal fun ModelNoteEntity.toDto() = ModelNoteDto(modelId, noteText, createdAt, updatedAt)

internal fun PersonalTagEntity.toDto() = PersonalTagDto(modelId, tag, addedAt)

internal fun SavedPromptEntity.toDto() = SavedPromptDto(
    prompt, negativePrompt, sampler, steps, cfgScale, seed,
    modelName, size, sourceImageUrl, savedAt, isTemplate,
    templateName, autoSaved, templateVariables, templateType, templateMetadata,
)

internal fun UserPreferencesEntity.toDto() = UserPreferencesDto(
    nsfwFilterLevel, defaultSortOrder, defaultTimePeriod, gridColumns,
    powerUserMode, notificationsEnabled, pollingIntervalMinutes,
    nsfwBlurSoft, nsfwBlurMature, nsfwBlurExplicit, offlineCacheEnabled,
    cacheSizeLimitMb, accentColor, amoledDarkMode, themeMode, customNavShortcuts,
)

internal fun SavedSearchFilterEntity.toDto() = SavedSearchFilterDto(
    name, query, selectedType, selectedSort, selectedPeriod,
    selectedBaseModels, nsfwFilterLevel, isFreshFindEnabled,
    excludedTags, includedTags, selectedSources, savedAt,
)

internal fun FollowedCreatorEntity.toDto() = FollowedCreatorDto(
    username,
    displayName,
    avatarUrl,
    followedAt,
    lastCheckedAt,
)

internal fun ComfyUIConnectionEntity.toDto() = ComfyUIConnectionDto(
    name,
    hostname,
    port,
    isActive,
    createdAt,
)

internal fun SDWebUIConnectionEntity.toDto() = SDWebUIConnectionDto(
    name,
    hostname,
    port,
    isActive,
    createdAt,
)

internal fun ExternalServerConfigEntity.toDto() = ExternalServerConfigDto(
    name,
    baseUrl,
    apiKey,
    isActive,
    createdAt,
)

internal fun HiddenModelEntity.toDto() = HiddenModelDto(modelId, modelName, hiddenAt)

internal fun ExcludedTagEntity.toDto() = ExcludedTagDto(tag, addedAt)

// --- DTO -> Entity mappers ---

internal fun CollectionDto.toEntity() = CollectionEntity(id, name, isDefault, createdAt, updatedAt)

internal fun CollectionModelDto.toEntity() = CollectionModelEntity(
    collectionId, modelId, name, type, nsfw, thumbnailUrl,
    creatorName, downloadCount, favoriteCount, rating, addedAt,
)

internal fun ModelNoteDto.toEntity() = ModelNoteEntity(
    modelId = modelId,
    noteText = noteText,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun PersonalTagDto.toEntity() = PersonalTagEntity(
    modelId = modelId,
    tag = tag,
    addedAt = addedAt,
)

internal fun SavedPromptDto.toEntity() = SavedPromptEntity(
    prompt = prompt, negativePrompt = negativePrompt, sampler = sampler,
    steps = steps, cfgScale = cfgScale, seed = seed, modelName = modelName,
    size = size, sourceImageUrl = sourceImageUrl, savedAt = savedAt,
    isTemplate = isTemplate, templateName = templateName, autoSaved = autoSaved,
    templateVariables = templateVariables, templateType = templateType,
    templateMetadata = templateMetadata,
)

internal fun UserPreferencesDto.toEntity() = UserPreferencesEntity(
    nsfwFilterLevel = nsfwFilterLevel, defaultSortOrder = defaultSortOrder,
    defaultTimePeriod = defaultTimePeriod, gridColumns = gridColumns,
    powerUserMode = powerUserMode, notificationsEnabled = notificationsEnabled,
    pollingIntervalMinutes = pollingIntervalMinutes, nsfwBlurSoft = nsfwBlurSoft,
    nsfwBlurMature = nsfwBlurMature, nsfwBlurExplicit = nsfwBlurExplicit,
    offlineCacheEnabled = offlineCacheEnabled, cacheSizeLimitMb = cacheSizeLimitMb,
    accentColor = accentColor, amoledDarkMode = amoledDarkMode,
    themeMode = themeMode, customNavShortcuts = customNavShortcuts,
)

internal fun SavedSearchFilterDto.toEntity() = SavedSearchFilterEntity(
    name = name, query = query, selectedType = selectedType,
    selectedSort = selectedSort, selectedPeriod = selectedPeriod,
    selectedBaseModels = selectedBaseModels, nsfwFilterLevel = nsfwFilterLevel,
    isFreshFindEnabled = isFreshFindEnabled, excludedTags = excludedTags,
    includedTags = includedTags, selectedSources = selectedSources, savedAt = savedAt,
)

internal fun FollowedCreatorDto.toEntity() = FollowedCreatorEntity(
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    followedAt = followedAt,
    lastCheckedAt = lastCheckedAt,
)

internal fun ComfyUIConnectionDto.toEntity() = ComfyUIConnectionEntity(
    name = name,
    hostname = hostname,
    port = port,
    isActive = isActive,
    createdAt = createdAt,
)

internal fun SDWebUIConnectionDto.toEntity() = SDWebUIConnectionEntity(
    name = name,
    hostname = hostname,
    port = port,
    isActive = isActive,
    createdAt = createdAt,
)

internal fun ExternalServerConfigDto.toEntity() = ExternalServerConfigEntity(
    name = name,
    baseUrl = baseUrl,
    apiKey = apiKey,
    isActive = isActive,
    createdAt = createdAt,
)

internal fun HiddenModelDto.toEntity() = HiddenModelEntity(
    modelId = modelId,
    modelName = modelName,
    hiddenAt = hiddenAt,
)

internal fun ExcludedTagDto.toEntity() = ExcludedTagEntity(tag = tag, addedAt = addedAt)
