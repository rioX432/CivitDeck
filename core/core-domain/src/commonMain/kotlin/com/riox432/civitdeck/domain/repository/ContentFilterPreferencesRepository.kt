package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import kotlinx.coroutines.flow.Flow

interface ContentFilterPreferencesRepository {
    fun observeNsfwFilterLevel(): Flow<NsfwFilterLevel>
    suspend fun setNsfwFilterLevel(level: NsfwFilterLevel)
    fun observeNsfwBlurSettings(): Flow<NsfwBlurSettings>
    suspend fun setNsfwBlurSettings(settings: NsfwBlurSettings)
}
