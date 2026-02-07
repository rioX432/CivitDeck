package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.dao.UserPreferencesDao
import com.riox432.civitdeck.data.local.entity.UserPreferencesEntity
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
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
}
