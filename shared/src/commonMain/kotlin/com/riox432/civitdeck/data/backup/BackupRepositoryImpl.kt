package com.riox432.civitdeck.data.backup

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.CollectionDao
import com.riox432.civitdeck.data.local.dao.ComfyUIConnectionDao
import com.riox432.civitdeck.data.local.dao.ExcludedTagDao
import com.riox432.civitdeck.data.local.dao.ExternalServerConfigDao
import com.riox432.civitdeck.data.local.dao.FollowedCreatorDao
import com.riox432.civitdeck.data.local.dao.HiddenModelDao
import com.riox432.civitdeck.data.local.dao.ModelNoteDao
import com.riox432.civitdeck.data.local.dao.PersonalTagDao
import com.riox432.civitdeck.data.local.dao.SDWebUIConnectionDao
import com.riox432.civitdeck.data.local.dao.SavedPromptDao
import com.riox432.civitdeck.data.local.dao.SavedSearchFilterDao
import com.riox432.civitdeck.data.local.dao.UserPreferencesDao
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
import com.riox432.civitdeck.domain.model.BackupCategory
import com.riox432.civitdeck.domain.model.RestoreStrategy
import com.riox432.civitdeck.domain.repository.BackupRepository
import kotlinx.serialization.json.Json

/** Groups collection-related DAOs. */
data class CollectionDaos(
    val collectionDao: CollectionDao,
)

/** Groups server connection DAOs. */
data class ConnectionDaos(
    val comfyUIConnectionDao: ComfyUIConnectionDao,
    val sdWebUIConnectionDao: SDWebUIConnectionDao,
    val externalServerConfigDao: ExternalServerConfigDao,
)

/** Groups user-generated content DAOs (prompts, notes, tags, filters, creators). */
data class ContentDaos(
    val savedPromptDao: SavedPromptDao,
    val modelNoteDao: ModelNoteDao,
    val personalTagDao: PersonalTagDao,
    val savedSearchFilterDao: SavedSearchFilterDao,
    val followedCreatorDao: FollowedCreatorDao,
)

/** Groups user preferences and content moderation DAOs. */
data class PreferenceDaos(
    val userPreferencesDao: UserPreferencesDao,
    val hiddenModelDao: HiddenModelDao,
    val excludedTagDao: ExcludedTagDao,
)

class BackupRepositoryImpl(
    private val collectionDaos: CollectionDaos,
    private val connectionDaos: ConnectionDaos,
    private val contentDaos: ContentDaos,
    private val preferenceDaos: PreferenceDaos,
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun createBackup(categories: Set<BackupCategory>): String {
        val backup = BackupDto(
            metadata = BackupMetadataDto(
                createdAt = currentTimeMillis(),
                categories = categories.map { it.name },
            ),
            collections = exportCollections(categories),
            collectionModels = exportCollectionModels(categories),
            modelNotes = exportNotes(categories),
            personalTags = exportTags(categories),
            savedPrompts = exportPrompts(categories),
            userPreferences = exportSettings(categories),
            savedSearchFilters = exportSearchFilters(categories),
            followedCreators = exportFollowedCreators(categories),
            comfyUIConnections = exportComfyUIConnections(categories),
            sdWebUIConnections = exportSDWebUIConnections(categories),
            externalServerConfigs = exportExternalServers(categories),
            hiddenModels = exportHiddenModels(categories),
            excludedTags = exportExcludedTags(categories),
        )
        return json.encodeToString(BackupDto.serializer(), backup)
    }

    override suspend fun restoreBackup(
        json: String,
        strategy: RestoreStrategy,
        categories: Set<BackupCategory>,
    ) {
        val backup = this.json.decodeFromString(BackupDto.serializer(), json)
        restoreCollectionData(backup, strategy, categories)
        restoreContentData(backup, strategy, categories)
        restorePreferenceData(backup, strategy, categories)
        restoreConnectionData(backup, strategy, categories)
    }

    override suspend fun parseBackupCategories(json: String): Set<BackupCategory> {
        val backup = this.json.decodeFromString(BackupDto.serializer(), json)
        return backup.metadata.categories.mapNotNull { name ->
            runCatching { BackupCategory.valueOf(name) }.getOrNull()
        }.toSet()
    }

    // --- Export helpers ---

    private suspend fun exportCollections(categories: Set<BackupCategory>): List<CollectionDto>? =
        exportIf(BackupCategory.COLLECTIONS in categories) {
            collectionDaos.collectionDao.getAll().map { it.toDto() }
        }

    private suspend fun exportCollectionModels(
        categories: Set<BackupCategory>,
    ): List<CollectionModelDto>? =
        exportIf(BackupCategory.COLLECTIONS in categories) {
            collectionDaos.collectionDao.getAllEntries().map { it.toDto() }
        }

    private suspend fun exportNotes(categories: Set<BackupCategory>): List<ModelNoteDto>? =
        exportIf(BackupCategory.NOTES in categories) {
            contentDaos.modelNoteDao.getAll().map { it.toDto() }
        }

    private suspend fun exportTags(categories: Set<BackupCategory>): List<PersonalTagDto>? =
        exportIf(BackupCategory.TAGS in categories) {
            contentDaos.personalTagDao.getAll().map { it.toDto() }
        }

    private suspend fun exportPrompts(categories: Set<BackupCategory>): List<SavedPromptDto>? =
        exportIf(BackupCategory.PROMPTS in categories) {
            contentDaos.savedPromptDao.getAllUserCreated().map { it.toDto() }
        }

    private suspend fun exportSettings(
        categories: Set<BackupCategory>,
    ): UserPreferencesDto? =
        exportIf(BackupCategory.SETTINGS in categories) {
            preferenceDaos.userPreferencesDao.getPreferences()?.toDto()
        }

    private suspend fun exportSearchFilters(
        categories: Set<BackupCategory>,
    ): List<SavedSearchFilterDto>? =
        exportIf(BackupCategory.SEARCH_FILTERS in categories) {
            contentDaos.savedSearchFilterDao.getAll().map { it.toDto() }
        }

    private suspend fun exportFollowedCreators(
        categories: Set<BackupCategory>,
    ): List<FollowedCreatorDto>? =
        exportIf(BackupCategory.FOLLOWED_CREATORS in categories) {
            contentDaos.followedCreatorDao.getAll().map { it.toDto() }
        }

    private suspend fun exportComfyUIConnections(
        categories: Set<BackupCategory>,
    ): List<ComfyUIConnectionDto>? =
        exportIf(BackupCategory.CONNECTIONS in categories) {
            connectionDaos.comfyUIConnectionDao.getAll().map { it.toDto() }
        }

    private suspend fun exportSDWebUIConnections(
        categories: Set<BackupCategory>,
    ): List<SDWebUIConnectionDto>? =
        exportIf(BackupCategory.CONNECTIONS in categories) {
            connectionDaos.sdWebUIConnectionDao.getAll().map { it.toDto() }
        }

    private suspend fun exportExternalServers(
        categories: Set<BackupCategory>,
    ): List<ExternalServerConfigDto>? =
        exportIf(BackupCategory.CONNECTIONS in categories) {
            connectionDaos.externalServerConfigDao.getAll().map { it.toDto() }
        }

    private suspend fun exportHiddenModels(
        categories: Set<BackupCategory>,
    ): List<HiddenModelDto>? =
        exportIf(BackupCategory.HIDDEN_MODELS in categories) {
            preferenceDaos.hiddenModelDao.getAll().map { it.toDto() }
        }

    private suspend fun exportExcludedTags(
        categories: Set<BackupCategory>,
    ): List<ExcludedTagDto>? =
        exportIf(BackupCategory.HIDDEN_MODELS in categories) {
            preferenceDaos.excludedTagDao.getAll().map { it.toDto() }
        }

    private suspend fun <T> exportIf(condition: Boolean, block: suspend () -> T): T? =
        if (condition) block() else null

    // --- Restore: category-group dispatchers ---

    private suspend fun restoreCollectionData(
        backup: BackupDto,
        strategy: RestoreStrategy,
        categories: Set<BackupCategory>,
    ) {
        if (BackupCategory.COLLECTIONS !in categories || backup.collections == null) return
        if (strategy == RestoreStrategy.OVERWRITE) {
            collectionDaos.collectionDao.deleteAllEntries()
            collectionDaos.collectionDao.deleteAllNonDefault()
        }
        backup.collections.filter { !it.isDefault }.map { it.toEntity() }.let {
            collectionDaos.collectionDao.insertCollections(it)
        }
        backup.collectionModels?.map { it.toEntity() }?.let {
            collectionDaos.collectionDao.insertEntries(it)
        }
    }

    private suspend fun restoreContentData(
        backup: BackupDto,
        strategy: RestoreStrategy,
        categories: Set<BackupCategory>,
    ) {
        restoreNotes(backup, strategy, categories)
        restoreTags(backup, strategy, categories)
        restorePrompts(backup, strategy, categories)
        restoreSearchFilters(backup, strategy, categories)
        restoreFollowedCreators(backup, strategy, categories)
    }

    private suspend fun restorePreferenceData(
        backup: BackupDto,
        strategy: RestoreStrategy,
        categories: Set<BackupCategory>,
    ) {
        if (BackupCategory.SETTINGS in categories && backup.userPreferences != null) {
            // Settings always overwrite (single row)
            preferenceDaos.userPreferencesDao.upsert(backup.userPreferences.toEntity())
        }
        if (BackupCategory.HIDDEN_MODELS in categories) {
            restoreHiddenModels(backup, strategy)
        }
    }

    private suspend fun restoreConnectionData(
        backup: BackupDto,
        strategy: RestoreStrategy,
        categories: Set<BackupCategory>,
    ) {
        if (BackupCategory.CONNECTIONS !in categories) return
        if (strategy == RestoreStrategy.OVERWRITE) {
            if (backup.comfyUIConnections != null) connectionDaos.comfyUIConnectionDao.deleteAll()
            if (backup.sdWebUIConnections != null) connectionDaos.sdWebUIConnectionDao.deleteAll()
            if (backup.externalServerConfigs != null) {
                connectionDaos.externalServerConfigDao.deleteAll()
            }
        }
        backup.comfyUIConnections?.let {
            connectionDaos.comfyUIConnectionDao.insertAll(it.map { c -> c.toEntity() })
        }
        backup.sdWebUIConnections?.let {
            connectionDaos.sdWebUIConnectionDao.insertAll(it.map { c -> c.toEntity() })
        }
        backup.externalServerConfigs?.let {
            connectionDaos.externalServerConfigDao.insertAll(it.map { c -> c.toEntity() })
        }
    }

    // --- Restore: per-category helpers ---

    private suspend fun restoreNotes(
        backup: BackupDto,
        strategy: RestoreStrategy,
        categories: Set<BackupCategory>,
    ) {
        if (BackupCategory.NOTES !in categories || backup.modelNotes == null) return
        if (strategy == RestoreStrategy.OVERWRITE) contentDaos.modelNoteDao.deleteAll()
        contentDaos.modelNoteDao.insertAll(backup.modelNotes.map { it.toEntity() })
    }

    private suspend fun restoreTags(
        backup: BackupDto,
        strategy: RestoreStrategy,
        categories: Set<BackupCategory>,
    ) {
        if (BackupCategory.TAGS !in categories || backup.personalTags == null) return
        if (strategy == RestoreStrategy.OVERWRITE) contentDaos.personalTagDao.deleteAll()
        contentDaos.personalTagDao.insertAll(backup.personalTags.map { it.toEntity() })
    }

    private suspend fun restorePrompts(
        backup: BackupDto,
        strategy: RestoreStrategy,
        categories: Set<BackupCategory>,
    ) {
        if (BackupCategory.PROMPTS !in categories || backup.savedPrompts == null) return
        if (strategy == RestoreStrategy.OVERWRITE) contentDaos.savedPromptDao.deleteAllUserCreated()
        contentDaos.savedPromptDao.insertAll(backup.savedPrompts.map { it.toEntity() })
    }

    private suspend fun restoreSearchFilters(
        backup: BackupDto,
        strategy: RestoreStrategy,
        categories: Set<BackupCategory>,
    ) {
        if (BackupCategory.SEARCH_FILTERS !in categories || backup.savedSearchFilters == null) return
        if (strategy == RestoreStrategy.OVERWRITE) contentDaos.savedSearchFilterDao.deleteAll()
        contentDaos.savedSearchFilterDao.insertAll(backup.savedSearchFilters.map { it.toEntity() })
    }

    private suspend fun restoreFollowedCreators(
        backup: BackupDto,
        strategy: RestoreStrategy,
        categories: Set<BackupCategory>,
    ) {
        if (BackupCategory.FOLLOWED_CREATORS !in categories) return
        if (backup.followedCreators == null) return
        if (strategy == RestoreStrategy.OVERWRITE) contentDaos.followedCreatorDao.deleteAll()
        contentDaos.followedCreatorDao.insertAll(backup.followedCreators.map { it.toEntity() })
    }

    private suspend fun restoreHiddenModels(backup: BackupDto, strategy: RestoreStrategy) {
        backup.hiddenModels?.let { models ->
            if (strategy == RestoreStrategy.OVERWRITE) preferenceDaos.hiddenModelDao.deleteAll()
            preferenceDaos.hiddenModelDao.insertAll(models.map { it.toEntity() })
        }
        backup.excludedTags?.let { tags ->
            if (strategy == RestoreStrategy.OVERWRITE) preferenceDaos.excludedTagDao.deleteAll()
            preferenceDaos.excludedTagDao.insertAll(tags.map { it.toEntity() })
        }
    }
}

// --- Entity -> DTO mappers ---

private fun CollectionEntity.toDto() = CollectionDto(id, name, isDefault, createdAt, updatedAt)

private fun CollectionModelEntity.toDto() = CollectionModelDto(
    collectionId, modelId, name, type, nsfw, thumbnailUrl,
    creatorName, downloadCount, favoriteCount, rating, addedAt,
)

private fun ModelNoteEntity.toDto() = ModelNoteDto(modelId, noteText, createdAt, updatedAt)

private fun PersonalTagEntity.toDto() = PersonalTagDto(modelId, tag, addedAt)

private fun SavedPromptEntity.toDto() = SavedPromptDto(
    prompt, negativePrompt, sampler, steps, cfgScale, seed,
    modelName, size, sourceImageUrl, savedAt, isTemplate,
    templateName, autoSaved, templateVariables, templateType,
)

private fun UserPreferencesEntity.toDto() = UserPreferencesDto(
    nsfwFilterLevel, defaultSortOrder, defaultTimePeriod, gridColumns,
    powerUserMode, notificationsEnabled, pollingIntervalMinutes,
    nsfwBlurSoft, nsfwBlurMature, nsfwBlurExplicit, offlineCacheEnabled,
    cacheSizeLimitMb, accentColor, amoledDarkMode, themeMode, customNavShortcuts,
)

private fun SavedSearchFilterEntity.toDto() = SavedSearchFilterDto(
    name, query, selectedType, selectedSort, selectedPeriod,
    selectedBaseModels, nsfwFilterLevel, isFreshFindEnabled,
    excludedTags, includedTags, selectedSources, savedAt,
)

private fun FollowedCreatorEntity.toDto() = FollowedCreatorDto(
    username,
    displayName,
    avatarUrl,
    followedAt,
    lastCheckedAt,
)

private fun ComfyUIConnectionEntity.toDto() = ComfyUIConnectionDto(
    name,
    hostname,
    port,
    isActive,
    createdAt,
)

private fun SDWebUIConnectionEntity.toDto() = SDWebUIConnectionDto(
    name,
    hostname,
    port,
    isActive,
    createdAt,
)

private fun ExternalServerConfigEntity.toDto() = ExternalServerConfigDto(
    name,
    baseUrl,
    apiKey,
    isActive,
    createdAt,
)

private fun HiddenModelEntity.toDto() = HiddenModelDto(modelId, modelName, hiddenAt)

private fun ExcludedTagEntity.toDto() = ExcludedTagDto(tag, addedAt)

// --- DTO -> Entity mappers ---

private fun CollectionDto.toEntity() = CollectionEntity(id, name, isDefault, createdAt, updatedAt)

private fun CollectionModelDto.toEntity() = CollectionModelEntity(
    collectionId, modelId, name, type, nsfw, thumbnailUrl,
    creatorName, downloadCount, favoriteCount, rating, addedAt,
)

private fun ModelNoteDto.toEntity() = ModelNoteEntity(
    modelId = modelId,
    noteText = noteText,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun PersonalTagDto.toEntity() = PersonalTagEntity(
    modelId = modelId,
    tag = tag,
    addedAt = addedAt,
)

private fun SavedPromptDto.toEntity() = SavedPromptEntity(
    prompt = prompt, negativePrompt = negativePrompt, sampler = sampler,
    steps = steps, cfgScale = cfgScale, seed = seed, modelName = modelName,
    size = size, sourceImageUrl = sourceImageUrl, savedAt = savedAt,
    isTemplate = isTemplate, templateName = templateName, autoSaved = autoSaved,
    templateVariables = templateVariables, templateType = templateType,
)

private fun UserPreferencesDto.toEntity() = UserPreferencesEntity(
    nsfwFilterLevel = nsfwFilterLevel, defaultSortOrder = defaultSortOrder,
    defaultTimePeriod = defaultTimePeriod, gridColumns = gridColumns,
    powerUserMode = powerUserMode, notificationsEnabled = notificationsEnabled,
    pollingIntervalMinutes = pollingIntervalMinutes, nsfwBlurSoft = nsfwBlurSoft,
    nsfwBlurMature = nsfwBlurMature, nsfwBlurExplicit = nsfwBlurExplicit,
    offlineCacheEnabled = offlineCacheEnabled, cacheSizeLimitMb = cacheSizeLimitMb,
    accentColor = accentColor, amoledDarkMode = amoledDarkMode,
    themeMode = themeMode, customNavShortcuts = customNavShortcuts,
)

private fun SavedSearchFilterDto.toEntity() = SavedSearchFilterEntity(
    name = name, query = query, selectedType = selectedType,
    selectedSort = selectedSort, selectedPeriod = selectedPeriod,
    selectedBaseModels = selectedBaseModels, nsfwFilterLevel = nsfwFilterLevel,
    isFreshFindEnabled = isFreshFindEnabled, excludedTags = excludedTags,
    includedTags = includedTags, selectedSources = selectedSources, savedAt = savedAt,
)

private fun FollowedCreatorDto.toEntity() = FollowedCreatorEntity(
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    followedAt = followedAt,
    lastCheckedAt = lastCheckedAt,
)

private fun ComfyUIConnectionDto.toEntity() = ComfyUIConnectionEntity(
    name = name,
    hostname = hostname,
    port = port,
    isActive = isActive,
    createdAt = createdAt,
)

private fun SDWebUIConnectionDto.toEntity() = SDWebUIConnectionEntity(
    name = name,
    hostname = hostname,
    port = port,
    isActive = isActive,
    createdAt = createdAt,
)

private fun ExternalServerConfigDto.toEntity() = ExternalServerConfigEntity(
    name = name,
    baseUrl = baseUrl,
    apiKey = apiKey,
    isActive = isActive,
    createdAt = createdAt,
)

private fun HiddenModelDto.toEntity() = HiddenModelEntity(
    modelId = modelId,
    modelName = modelName,
    hiddenAt = hiddenAt,
)

private fun ExcludedTagDto.toEntity() = ExcludedTagEntity(tag = tag, addedAt = addedAt)
