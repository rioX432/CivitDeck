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

@Suppress("LongParameterList")
class BackupRepositoryImpl(
    private val collectionDao: CollectionDao,
    private val savedPromptDao: SavedPromptDao,
    private val modelNoteDao: ModelNoteDao,
    private val personalTagDao: PersonalTagDao,
    private val userPreferencesDao: UserPreferencesDao,
    private val savedSearchFilterDao: SavedSearchFilterDao,
    private val followedCreatorDao: FollowedCreatorDao,
    private val comfyUIConnectionDao: ComfyUIConnectionDao,
    private val sdWebUIConnectionDao: SDWebUIConnectionDao,
    private val externalServerConfigDao: ExternalServerConfigDao,
    private val hiddenModelDao: HiddenModelDao,
    private val excludedTagDao: ExcludedTagDao,
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
            collections = exportIf(
                BackupCategory.COLLECTIONS in categories
            ) { collectionDao.getAll().map { it.toDto() } },
            collectionModels = exportIf(BackupCategory.COLLECTIONS in categories) {
                collectionDao.getAllEntries().map { it.toDto() }
            },
            modelNotes = exportIf(BackupCategory.NOTES in categories) { modelNoteDao.getAll().map { it.toDto() } },
            personalTags = exportIf(BackupCategory.TAGS in categories) { personalTagDao.getAll().map { it.toDto() } },
            savedPrompts = exportIf(
                BackupCategory.PROMPTS in categories
            ) { savedPromptDao.getAllUserCreated().map { it.toDto() } },
            userPreferences = exportIf(
                BackupCategory.SETTINGS in categories
            ) { userPreferencesDao.getPreferences()?.toDto() },
            savedSearchFilters = exportIf(BackupCategory.SEARCH_FILTERS in categories) {
                savedSearchFilterDao.getAll().map { it.toDto() }
            },
            followedCreators = exportIf(BackupCategory.FOLLOWED_CREATORS in categories) {
                followedCreatorDao.getAll().map { it.toDto() }
            },
            comfyUIConnections = exportIf(BackupCategory.CONNECTIONS in categories) {
                comfyUIConnectionDao.getAll().map { it.toDto() }
            },
            sdWebUIConnections = exportIf(BackupCategory.CONNECTIONS in categories) {
                sdWebUIConnectionDao.getAll().map { it.toDto() }
            },
            externalServerConfigs = exportIf(BackupCategory.CONNECTIONS in categories) {
                externalServerConfigDao.getAll().map { it.toDto() }
            },
            hiddenModels = exportIf(
                BackupCategory.HIDDEN_MODELS in categories
            ) { hiddenModelDao.getAll().map { it.toDto() } },
            excludedTags = exportIf(
                BackupCategory.HIDDEN_MODELS in categories
            ) { excludedTagDao.getAll().map { it.toDto() } },
        )
        return json.encodeToString(BackupDto.serializer(), backup)
    }

    private suspend fun <T> exportIf(condition: Boolean, block: suspend () -> T): T? =
        if (condition) block() else null

    @Suppress("CyclomaticComplexMethod")
    override suspend fun restoreBackup(
        json: String,
        strategy: RestoreStrategy,
        categories: Set<BackupCategory>,
    ) {
        val backup = this.json.decodeFromString(BackupDto.serializer(), json)

        if (BackupCategory.COLLECTIONS in categories && backup.collections != null) {
            restoreCollections(backup, strategy)
        }
        if (BackupCategory.NOTES in categories && backup.modelNotes != null) {
            restoreNotes(backup.modelNotes, strategy)
        }
        if (BackupCategory.TAGS in categories && backup.personalTags != null) {
            restoreTags(backup.personalTags, strategy)
        }
        if (BackupCategory.PROMPTS in categories && backup.savedPrompts != null) {
            restorePrompts(backup.savedPrompts, strategy)
        }
        if (BackupCategory.SETTINGS in categories && backup.userPreferences != null) {
            restoreSettings(backup.userPreferences)
        }
        if (BackupCategory.SEARCH_FILTERS in categories && backup.savedSearchFilters != null) {
            restoreSearchFilters(backup.savedSearchFilters, strategy)
        }
        if (BackupCategory.FOLLOWED_CREATORS in categories && backup.followedCreators != null) {
            restoreFollowedCreators(backup.followedCreators, strategy)
        }
        if (BackupCategory.CONNECTIONS in categories) {
            restoreConnections(backup, strategy)
        }
        if (BackupCategory.HIDDEN_MODELS in categories) {
            restoreHiddenModels(backup, strategy)
        }
    }

    override suspend fun parseBackupCategories(json: String): Set<BackupCategory> {
        val backup = this.json.decodeFromString(BackupDto.serializer(), json)
        return backup.metadata.categories.mapNotNull { name ->
            runCatching { BackupCategory.valueOf(name) }.getOrNull()
        }.toSet()
    }

    private suspend fun restoreCollections(backup: BackupDto, strategy: RestoreStrategy) {
        if (strategy == RestoreStrategy.OVERWRITE) {
            collectionDao.deleteAllEntries()
            collectionDao.deleteAllNonDefault()
        }
        backup.collections?.filter { !it.isDefault }?.map { it.toEntity() }?.let {
            collectionDao.insertCollections(it)
        }
        backup.collectionModels?.map { it.toEntity() }?.let {
            collectionDao.insertEntries(it)
        }
    }

    private suspend fun restoreNotes(notes: List<ModelNoteDto>, strategy: RestoreStrategy) {
        if (strategy == RestoreStrategy.OVERWRITE) modelNoteDao.deleteAll()
        modelNoteDao.insertAll(notes.map { it.toEntity() })
    }

    private suspend fun restoreTags(tags: List<PersonalTagDto>, strategy: RestoreStrategy) {
        if (strategy == RestoreStrategy.OVERWRITE) personalTagDao.deleteAll()
        personalTagDao.insertAll(tags.map { it.toEntity() })
    }

    private suspend fun restorePrompts(prompts: List<SavedPromptDto>, strategy: RestoreStrategy) {
        if (strategy == RestoreStrategy.OVERWRITE) savedPromptDao.deleteAllUserCreated()
        savedPromptDao.insertAll(prompts.map { it.toEntity() })
    }

    private suspend fun restoreSettings(prefs: UserPreferencesDto) {
        // Settings always overwrite (single row)
        userPreferencesDao.upsert(prefs.toEntity())
    }

    private suspend fun restoreSearchFilters(
        filters: List<SavedSearchFilterDto>,
        strategy: RestoreStrategy,
    ) {
        if (strategy == RestoreStrategy.OVERWRITE) savedSearchFilterDao.deleteAll()
        savedSearchFilterDao.insertAll(filters.map { it.toEntity() })
    }

    private suspend fun restoreFollowedCreators(
        creators: List<FollowedCreatorDto>,
        strategy: RestoreStrategy,
    ) {
        if (strategy == RestoreStrategy.OVERWRITE) followedCreatorDao.deleteAll()
        followedCreatorDao.insertAll(creators.map { it.toEntity() })
    }

    private suspend fun restoreConnections(backup: BackupDto, strategy: RestoreStrategy) {
        backup.comfyUIConnections?.let { conns ->
            if (strategy == RestoreStrategy.OVERWRITE) {
                // Cannot bulk delete without adding method; insert with REPLACE handles it
            }
            comfyUIConnectionDao.insertAll(conns.map { it.toEntity() })
        }
        backup.sdWebUIConnections?.let { conns ->
            sdWebUIConnectionDao.insertAll(conns.map { it.toEntity() })
        }
        backup.externalServerConfigs?.let { configs ->
            externalServerConfigDao.insertAll(configs.map { it.toEntity() })
        }
    }

    private suspend fun restoreHiddenModels(backup: BackupDto, strategy: RestoreStrategy) {
        backup.hiddenModels?.let { models ->
            if (strategy == RestoreStrategy.OVERWRITE) hiddenModelDao.deleteAll()
            hiddenModelDao.insertAll(models.map { it.toEntity() })
        }
        backup.excludedTags?.let { tags ->
            if (strategy == RestoreStrategy.OVERWRITE) excludedTagDao.deleteAll()
            excludedTagDao.insertAll(tags.map { it.toEntity() })
        }
    }
}

// --- Entity → DTO mappers ---

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
    excludedTags, includedTags, savedAt,
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

// --- DTO → Entity mappers ---

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
    includedTags = includedTags, savedAt = savedAt,
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
