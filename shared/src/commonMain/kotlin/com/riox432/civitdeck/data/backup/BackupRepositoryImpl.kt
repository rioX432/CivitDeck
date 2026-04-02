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

    // region Public API

    override suspend fun createBackup(categories: Set<BackupCategory>): String {
        val backup = BackupDto(
            metadata = BackupMetadataDto(
                createdAt = currentTimeMillis(),
                categories = categories.map { it.name },
            ),
            collections = exportIf(BackupCategory.COLLECTIONS, categories) {
                collectionDaos.collectionDao.getAll().map { it.toDto() }
            },
            collectionModels = exportIf(BackupCategory.COLLECTIONS, categories) {
                collectionDaos.collectionDao.getAllEntries().map { it.toDto() }
            },
            modelNotes = exportIf(BackupCategory.NOTES, categories) {
                contentDaos.modelNoteDao.getAll().map { it.toDto() }
            },
            personalTags = exportIf(BackupCategory.TAGS, categories) {
                contentDaos.personalTagDao.getAll().map { it.toDto() }
            },
            savedPrompts = exportIf(BackupCategory.PROMPTS, categories) {
                contentDaos.savedPromptDao.getAllUserCreated().map { it.toDto() }
            },
            userPreferences = exportIf(BackupCategory.SETTINGS, categories) {
                preferenceDaos.userPreferencesDao.getPreferences()?.toDto()
            },
            savedSearchFilters = exportIf(BackupCategory.SEARCH_FILTERS, categories) {
                contentDaos.savedSearchFilterDao.getAll().map { it.toDto() }
            },
            followedCreators = exportIf(BackupCategory.FOLLOWED_CREATORS, categories) {
                contentDaos.followedCreatorDao.getAll().map { it.toDto() }
            },
            comfyUIConnections = exportIf(BackupCategory.CONNECTIONS, categories) {
                connectionDaos.comfyUIConnectionDao.getAll().map { it.toDto() }
            },
            sdWebUIConnections = exportIf(BackupCategory.CONNECTIONS, categories) {
                connectionDaos.sdWebUIConnectionDao.getAll().map { it.toDto() }
            },
            externalServerConfigs = exportIf(BackupCategory.CONNECTIONS, categories) {
                connectionDaos.externalServerConfigDao.getAll().map { it.toDto() }
            },
            hiddenModels = exportIf(BackupCategory.HIDDEN_MODELS, categories) {
                preferenceDaos.hiddenModelDao.getAll().map { it.toDto() }
            },
            excludedTags = exportIf(BackupCategory.HIDDEN_MODELS, categories) {
                preferenceDaos.excludedTagDao.getAll().map { it.toDto() }
            },
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

    // endregion

    // region Export helper

    private suspend fun <T> exportIf(
        category: BackupCategory,
        categories: Set<BackupCategory>,
        block: suspend () -> T,
    ): T? = if (category in categories) block() else null

    // endregion

    // region Restore helpers

    /**
     * Generic restore: checks category membership and null data, optionally clears
     * existing rows on OVERWRITE, then bulk-inserts the mapped entities.
     */
    private suspend fun <D, E> restoreList(
        data: List<D>?,
        category: BackupCategory,
        categories: Set<BackupCategory>,
        strategy: RestoreStrategy,
        deleteAll: suspend () -> Unit,
        insertAll: suspend (List<E>) -> Unit,
        toEntity: (D) -> E,
    ) {
        if (category !in categories || data == null) return
        if (strategy == RestoreStrategy.OVERWRITE) deleteAll()
        insertAll(data.map(toEntity))
    }

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
        restoreList(
            backup.modelNotes,
            BackupCategory.NOTES,
            categories,
            strategy,
            deleteAll = { contentDaos.modelNoteDao.deleteAll() },
            insertAll = { contentDaos.modelNoteDao.insertAll(it) },
            toEntity = { it.toEntity() },
        )
        restoreList(
            backup.personalTags,
            BackupCategory.TAGS,
            categories,
            strategy,
            deleteAll = { contentDaos.personalTagDao.deleteAll() },
            insertAll = { contentDaos.personalTagDao.insertAll(it) },
            toEntity = { it.toEntity() },
        )
        restoreList(
            backup.savedPrompts,
            BackupCategory.PROMPTS,
            categories,
            strategy,
            deleteAll = { contentDaos.savedPromptDao.deleteAllUserCreated() },
            insertAll = { contentDaos.savedPromptDao.insertAll(it) },
            toEntity = { it.toEntity() },
        )
        restoreList(
            backup.savedSearchFilters,
            BackupCategory.SEARCH_FILTERS,
            categories,
            strategy,
            deleteAll = { contentDaos.savedSearchFilterDao.deleteAll() },
            insertAll = { contentDaos.savedSearchFilterDao.insertAll(it) },
            toEntity = { it.toEntity() },
        )
        restoreList(
            backup.followedCreators,
            BackupCategory.FOLLOWED_CREATORS,
            categories,
            strategy,
            deleteAll = { contentDaos.followedCreatorDao.deleteAll() },
            insertAll = { contentDaos.followedCreatorDao.insertAll(it) },
            toEntity = { it.toEntity() },
        )
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
            restoreList(
                backup.hiddenModels,
                BackupCategory.HIDDEN_MODELS,
                categories,
                strategy,
                deleteAll = { preferenceDaos.hiddenModelDao.deleteAll() },
                insertAll = { preferenceDaos.hiddenModelDao.insertAll(it) },
                toEntity = { it.toEntity() },
            )
            restoreList(
                backup.excludedTags,
                BackupCategory.HIDDEN_MODELS,
                categories,
                strategy,
                deleteAll = { preferenceDaos.excludedTagDao.deleteAll() },
                insertAll = { preferenceDaos.excludedTagDao.insertAll(it) },
                toEntity = { it.toEntity() },
            )
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

    // endregion
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
