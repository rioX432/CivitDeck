package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.ApiKeyProvider
import com.riox432.civitdeck.data.local.dao.UserPreferencesDao
import com.riox432.civitdeck.data.local.entity.UserPreferencesEntity
import com.riox432.civitdeck.domain.model.AccentColor
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

    override fun observeAccentColor(): Flow<AccentColor> =
        dao.observePreferences().map { entity ->
            runCatching { AccentColor.valueOf(entity?.accentColor ?: "") }
                .getOrDefault(AccentColor.Blue)
        }

    override suspend fun setAccentColor(color: AccentColor) {
        val existing = dao.getPreferences() ?: UserPreferencesEntity()
        dao.upsert(existing.copy(accentColor = color.name))
    }

    override fun observeAmoledDarkMode(): Flow<Boolean> =
        dao.observePreferences().map { it?.amoledDarkMode ?: false }

    override suspend fun setAmoledDarkMode(enabled: Boolean) {
        val existing = dao.getPreferences() ?: UserPreferencesEntity()
        dao.upsert(existing.copy(amoledDarkMode = enabled))
    }
}
