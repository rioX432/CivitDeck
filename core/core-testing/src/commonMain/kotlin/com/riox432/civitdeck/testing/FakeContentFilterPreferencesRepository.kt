package com.riox432.civitdeck.testing

import com.riox432.civitdeck.domain.model.FrontDoorMode
import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.repository.ContentFilterPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [ContentFilterPreferencesRepository] for ViewModel tests.
 *
 * The NSFW filter level flow is mutable so tests can emit changes and assert how
 * a ViewModel reacts (e.g. triggering a refresh / recommendations reload).
 */
class FakeContentFilterPreferencesRepository(
    nsfwFilterLevel: NsfwFilterLevel = NsfwFilterLevel.Off,
    blurSettings: NsfwBlurSettings = NsfwBlurSettings(),
    frontDoorMode: FrontDoorMode = FrontDoorMode.Sfw,
) : ContentFilterPreferencesRepository {

    val nsfwFilterLevelFlow = MutableStateFlow(nsfwFilterLevel)
    val blurSettingsFlow = MutableStateFlow(blurSettings)
    val frontDoorModeFlow = MutableStateFlow(frontDoorMode)

    override fun observeNsfwFilterLevel(): Flow<NsfwFilterLevel> = nsfwFilterLevelFlow
    override suspend fun setNsfwFilterLevel(level: NsfwFilterLevel) {
        nsfwFilterLevelFlow.value = level
    }

    override fun observeNsfwBlurSettings(): Flow<NsfwBlurSettings> = blurSettingsFlow
    override suspend fun setNsfwBlurSettings(settings: NsfwBlurSettings) {
        blurSettingsFlow.value = settings
    }

    override fun observeFrontDoorMode(): Flow<FrontDoorMode> = frontDoorModeFlow
    override suspend fun setFrontDoorMode(mode: FrontDoorMode) {
        frontDoorModeFlow.value = mode
    }
}
