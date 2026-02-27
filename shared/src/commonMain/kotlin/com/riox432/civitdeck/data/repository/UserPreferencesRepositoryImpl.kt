package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.ApiKeyProvider
import com.riox432.civitdeck.data.local.dao.UserPreferencesDao
import com.riox432.civitdeck.data.local.entity.UserPreferencesEntity
import com.riox432.civitdeck.data.local.entity.UserPreferencesEntity.Companion.DEFAULT_CACHE_SIZE_LIMIT_MB
import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.PollingInterval
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesRepositoryImpl(
    private val dao: UserPreferencesDao,
    private val apiKeyProvider: ApiKeyProvider,
) : UserPreferencesRepository {

    override fun observeNsfwFilterLevel(): Flow<NsfwFilterLevel> =
        dao.observePreferences().map { entity ->
            entity?.nsfwFilterLevel?.let { NsfwFilterLevel.valueOf(it) }
                ?: NsfwFilterLevel.Off
        }

    override suspend fun setNsfwFilterLevel(level: NsfwFilterLevel) {
        val existing = dao.getPreferences() ?: UserPreferencesEntity()
        dao.upsert(existing.copy(nsfwFilterLevel = level.name))
    }

    override fun observeDefaultSortOrder(): Flow<SortOrder> =
        dao.observePreferences().map { entity ->
            runCatching { SortOrder.valueOf(entity?.defaultSortOrder ?: "") }.getOrDefault(SortOrder.MostDownloaded)
        }

    override suspend fun setDefaultSortOrder(sort: SortOrder) {
        val existing = dao.getPreferences() ?: UserPreferencesEntity()
        dao.upsert(existing.copy(defaultSortOrder = sort.name))
    }

    override fun observeDefaultTimePeriod(): Flow<TimePeriod> =
        dao.observePreferences().map { entity ->
            runCatching { TimePeriod.valueOf(entity?.defaultTimePeriod ?: "") }.getOrDefault(TimePeriod.AllTime)
        }

    override suspend fun setDefaultTimePeriod(period: TimePeriod) {
        val existing = dao.getPreferences() ?: UserPreferencesEntity()
        dao.upsert(existing.copy(defaultTimePeriod = period.name))
    }

    override fun observeGridColumns(): Flow<Int> =
        dao.observePreferences().map { entity ->
            entity?.gridColumns ?: 2
        }

    override suspend fun setGridColumns(columns: Int) {
        val existing = dao.getPreferences() ?: UserPreferencesEntity()
        dao.upsert(existing.copy(gridColumns = columns))
    }

    override fun observeApiKey(): Flow<String?> =
        dao.observePreferences().map { it?.apiKey }

    override suspend fun setApiKey(apiKey: String?) {
        val existing = dao.getPreferences() ?: UserPreferencesEntity()
        dao.upsert(existing.copy(apiKey = apiKey))
        apiKeyProvider.apiKey = apiKey
    }

    override suspend fun getApiKey(): String? =
        dao.getPreferences()?.apiKey

    override fun observePowerUserMode(): Flow<Boolean> =
        dao.observePreferences().map { it?.powerUserMode ?: false }

    override suspend fun setPowerUserMode(enabled: Boolean) {
        val existing = dao.getPreferences() ?: UserPreferencesEntity()
        dao.upsert(existing.copy(powerUserMode = enabled))
    }

    override fun observeNotificationsEnabled(): Flow<Boolean> =
        dao.observePreferences().map { it?.notificationsEnabled ?: false }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        val existing = dao.getPreferences() ?: UserPreferencesEntity()
        dao.upsert(existing.copy(notificationsEnabled = enabled))
    }

    override fun observePollingInterval(): Flow<PollingInterval> =
        dao.observePreferences().map { entity ->
            PollingInterval.fromMinutes(entity?.pollingIntervalMinutes ?: 0)
        }

    override suspend fun setPollingInterval(interval: PollingInterval) {
        val existing = dao.getPreferences() ?: UserPreferencesEntity()
        dao.upsert(existing.copy(pollingIntervalMinutes = interval.minutes))
    }

    override fun observeNsfwBlurSettings(): Flow<NsfwBlurSettings> =
        dao.observePreferences().map { entity ->
            NsfwBlurSettings(
                softIntensity = entity?.nsfwBlurSoft ?: NsfwBlurSettings.DEFAULT_SOFT,
                matureIntensity = entity?.nsfwBlurMature ?: NsfwBlurSettings.DEFAULT_MATURE,
                explicitIntensity = entity?.nsfwBlurExplicit ?: NsfwBlurSettings.DEFAULT_EXPLICIT,
            )
        }

    override suspend fun setNsfwBlurSettings(settings: NsfwBlurSettings) {
        val existing = dao.getPreferences() ?: UserPreferencesEntity()
        dao.upsert(
            existing.copy(
                nsfwBlurSoft = settings.softIntensity,
                nsfwBlurMature = settings.matureIntensity,
                nsfwBlurExplicit = settings.explicitIntensity,
            ),
        )
    }

    override fun observeOfflineCacheEnabled(): Flow<Boolean> =
        dao.observePreferences().map { it?.offlineCacheEnabled ?: true }

    override suspend fun setOfflineCacheEnabled(enabled: Boolean) {
        val existing = dao.getPreferences() ?: UserPreferencesEntity()
        dao.upsert(existing.copy(offlineCacheEnabled = enabled))
    }

    override fun observeCacheSizeLimitMb(): Flow<Int> =
        dao.observePreferences().map { it?.cacheSizeLimitMb ?: DEFAULT_CACHE_SIZE_LIMIT_MB }

    override suspend fun setCacheSizeLimitMb(limitMb: Int) {
        val existing = dao.getPreferences() ?: UserPreferencesEntity()
        dao.upsert(existing.copy(cacheSizeLimitMb = limitMb))
    }

    override fun observeSeenTutorialVersion(): Flow<Int> =
        dao.observePreferences().map { it?.seenTutorialVersion ?: 0 }

    override suspend fun setSeenTutorialVersion(version: Int) {
        val existing = dao.getPreferences() ?: UserPreferencesEntity()
        dao.upsert(existing.copy(seenTutorialVersion = version))
    }
}
