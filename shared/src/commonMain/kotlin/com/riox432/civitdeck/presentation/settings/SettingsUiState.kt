package com.riox432.civitdeck.presentation.settings

import com.riox432.civitdeck.domain.model.AccentColor
import com.riox432.civitdeck.domain.model.CacheInfo
import com.riox432.civitdeck.domain.model.HiddenModel
import com.riox432.civitdeck.domain.model.NavShortcut
import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.PollingInterval
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.ThemeMode
import com.riox432.civitdeck.domain.model.TimePeriod

/**
 * Legacy aggregate UI state kept for backward compatibility with existing tests.
 * New code should use domain-scoped UiState classes instead:
 * [ContentFilterSettingsUiState], [DisplaySettingsUiState], [AppBehaviorSettingsUiState],
 * [AuthSettingsUiState], [StorageSettingsUiState].
 */
data class SettingsUiState(
    val nsfwFilterLevel: NsfwFilterLevel = NsfwFilterLevel.Off,
    val nsfwBlurSettings: NsfwBlurSettings = NsfwBlurSettings(),
    val defaultSortOrder: SortOrder = SortOrder.MostDownloaded,
    val defaultTimePeriod: TimePeriod = TimePeriod.AllTime,
    val gridColumns: Int = 2,
    val hiddenModels: List<HiddenModel> = emptyList(),
    val excludedTags: List<String> = emptyList(),
    val apiKey: String? = null,
    val connectedUsername: String? = null,
    val isValidatingApiKey: Boolean = false,
    val apiKeyError: String? = null,
    val powerUserMode: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val pollingInterval: PollingInterval = PollingInterval.Off,
    val accentColor: AccentColor = AccentColor.Blue,
    val amoledDarkMode: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isOnline: Boolean = true,
    val offlineCacheEnabled: Boolean = true,
    val cacheSizeLimitMb: Int = 200,
    val cacheInfo: CacheInfo = CacheInfo(0, 0),
    val customNavShortcuts: List<NavShortcut> = emptyList(),
)
