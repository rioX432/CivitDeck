package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.dao.UserPreferencesDao
import com.riox432.civitdeck.data.local.entity.UserPreferencesEntity
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesRepositoryImpl(
    private val dao: UserPreferencesDao,
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
}
