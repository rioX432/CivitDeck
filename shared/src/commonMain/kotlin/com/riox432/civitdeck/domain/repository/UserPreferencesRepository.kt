package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun observeNsfwFilterLevel(): Flow<NsfwFilterLevel>
    suspend fun setNsfwFilterLevel(level: NsfwFilterLevel)
}
