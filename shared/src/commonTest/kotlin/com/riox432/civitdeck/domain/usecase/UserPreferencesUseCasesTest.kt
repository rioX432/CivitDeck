package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.AccentColor
import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.PollingInterval
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.ThemeMode
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.AppBehaviorPreferencesRepository
import com.riox432.civitdeck.domain.repository.AuthPreferencesRepository
import com.riox432.civitdeck.domain.repository.ContentFilterPreferencesRepository
import com.riox432.civitdeck.domain.repository.DisplayPreferencesRepository
import com.riox432.civitdeck.domain.repository.StoragePreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Suppress("TooManyFunctions")
class UserPreferencesUseCasesTest {

    private class FakeUserPreferencesRepository :
        ContentFilterPreferencesRepository,
        DisplayPreferencesRepository,
        AuthPreferencesRepository,
        AppBehaviorPreferencesRepository,
        StoragePreferencesRepository {
        val nsfwLevel = MutableStateFlow(NsfwFilterLevel.Off)
        val sortOrder = MutableStateFlow(SortOrder.HighestRated)
        val timePeriod = MutableStateFlow(TimePeriod.AllTime)
        val gridColumns = MutableStateFlow(2)
        val apiKeyFlow = MutableStateFlow<String?>(null)
        var storedApiKey: String? = null

        override fun observeNsfwFilterLevel(): Flow<NsfwFilterLevel> = nsfwLevel
        override suspend fun setNsfwFilterLevel(level: NsfwFilterLevel) { nsfwLevel.value = level }
        override fun observeDefaultSortOrder(): Flow<SortOrder> = sortOrder
        override suspend fun setDefaultSortOrder(sort: SortOrder) { sortOrder.value = sort }
        override fun observeDefaultTimePeriod(): Flow<TimePeriod> = timePeriod
        override suspend fun setDefaultTimePeriod(period: TimePeriod) { timePeriod.value = period }
        override fun observeGridColumns(): Flow<Int> = gridColumns
        override suspend fun setGridColumns(columns: Int) { gridColumns.value = columns }
        override fun observeApiKey(): Flow<String?> = apiKeyFlow
        override suspend fun setApiKey(apiKey: String?) {
            storedApiKey = apiKey
            apiKeyFlow.value = apiKey
        }
        override suspend fun getApiKey(): String? = storedApiKey

        val powerUserMode = MutableStateFlow(false)
        override fun observePowerUserMode(): Flow<Boolean> = powerUserMode
        override suspend fun setPowerUserMode(enabled: Boolean) { powerUserMode.value = enabled }

        val notificationsEnabled = MutableStateFlow(false)
        override fun observeNotificationsEnabled(): Flow<Boolean> = notificationsEnabled
        override suspend fun setNotificationsEnabled(enabled: Boolean) { notificationsEnabled.value = enabled }

        val pollingInterval = MutableStateFlow(PollingInterval.Off)
        override fun observePollingInterval(): Flow<PollingInterval> = pollingInterval
        override suspend fun setPollingInterval(interval: PollingInterval) { pollingInterval.value = interval }

        val accentColor = MutableStateFlow(AccentColor.Blue)
        override fun observeAccentColor(): Flow<AccentColor> = accentColor
        override suspend fun setAccentColor(color: AccentColor) { accentColor.value = color }

        val amoledDarkMode = MutableStateFlow(false)
        override fun observeAmoledDarkMode(): Flow<Boolean> = amoledDarkMode
        override suspend fun setAmoledDarkMode(enabled: Boolean) { amoledDarkMode.value = enabled }
        val nsfwBlurSettings = MutableStateFlow(NsfwBlurSettings())
        override fun observeNsfwBlurSettings(): Flow<NsfwBlurSettings> = nsfwBlurSettings
        override suspend fun setNsfwBlurSettings(settings: NsfwBlurSettings) { nsfwBlurSettings.value = settings }

        val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
        override fun observeThemeMode(): Flow<ThemeMode> = themeMode
        override suspend fun setThemeMode(mode: ThemeMode) { themeMode.value = mode }

        val offlineCacheEnabled = MutableStateFlow(true)
        override fun observeOfflineCacheEnabled(): Flow<Boolean> = offlineCacheEnabled
        override suspend fun setOfflineCacheEnabled(enabled: Boolean) { offlineCacheEnabled.value = enabled }

        val cacheSizeLimitMb = MutableStateFlow(200)
        override fun observeCacheSizeLimitMb(): Flow<Int> = cacheSizeLimitMb
        override suspend fun setCacheSizeLimitMb(limitMb: Int) { cacheSizeLimitMb.value = limitMb }

        val seenTutorialVersion = MutableStateFlow(0)
        override fun observeSeenTutorialVersion(): Flow<Int> = seenTutorialVersion
        override suspend fun setSeenTutorialVersion(version: Int) { seenTutorialVersion.value = version }

        val civitaiLinkKey = MutableStateFlow<String?>(null)
        override fun observeCivitaiLinkKey(): Flow<String?> = civitaiLinkKey
        override suspend fun getCivitaiLinkKey(): String? = civitaiLinkKey.value
        override suspend fun setCivitaiLinkKey(key: String?) { civitaiLinkKey.value = key }
    }

    private val repo = FakeUserPreferencesRepository()

    // --- NSFW Filter ---

    @Test
    fun observeNsfwFilter_emits_current_level() = runTest {
        val useCase = ObserveNsfwFilterUseCase(repo)
        assertEquals(NsfwFilterLevel.Off, useCase().first())
    }

    @Test
    fun setNsfwFilter_updates_level() = runTest {
        val useCase = SetNsfwFilterUseCase(repo)
        useCase(NsfwFilterLevel.All)
        assertEquals(NsfwFilterLevel.All, repo.nsfwLevel.value)
    }

    // --- Sort Order ---

    @Test
    fun observeDefaultSortOrder_emits_current() = runTest {
        val useCase = ObserveDefaultSortOrderUseCase(repo)
        assertEquals(SortOrder.HighestRated, useCase().first())
    }

    @Test
    fun setDefaultSortOrder_updates() = runTest {
        val useCase = SetDefaultSortOrderUseCase(repo)
        useCase(SortOrder.MostDownloaded)
        assertEquals(SortOrder.MostDownloaded, repo.sortOrder.value)
    }

    // --- Time Period ---

    @Test
    fun observeDefaultTimePeriod_emits_current() = runTest {
        val useCase = ObserveDefaultTimePeriodUseCase(repo)
        assertEquals(TimePeriod.AllTime, useCase().first())
    }

    @Test
    fun setDefaultTimePeriod_updates() = runTest {
        val useCase = SetDefaultTimePeriodUseCase(repo)
        useCase(TimePeriod.Week)
        assertEquals(TimePeriod.Week, repo.timePeriod.value)
    }

    // --- Grid Columns ---

    @Test
    fun observeGridColumns_emits_current() = runTest {
        val useCase = ObserveGridColumnsUseCase(repo)
        assertEquals(2, useCase().first())
    }

    @Test
    fun setGridColumns_updates() = runTest {
        val useCase = SetGridColumnsUseCase(repo)
        useCase(3)
        assertEquals(3, repo.gridColumns.value)
    }

    // --- API Key ---

    @Test
    fun observeApiKey_emits_null_initially() = runTest {
        val useCase = ObserveApiKeyUseCase(repo)
        assertNull(useCase().first())
    }

    @Test
    fun setApiKey_stores_and_emits() = runTest {
        val setUseCase = SetApiKeyUseCase(repo)
        val observeUseCase = ObserveApiKeyUseCase(repo)
        setUseCase("my-key")
        assertEquals("my-key", observeUseCase().first())
        assertEquals("my-key", repo.storedApiKey)
    }

    @Test
    fun setApiKey_null_clears_key() = runTest {
        val setUseCase = SetApiKeyUseCase(repo)
        setUseCase("my-key")
        setUseCase(null)
        assertNull(repo.storedApiKey)
    }

    // --- Power User Mode ---

    @Test
    fun observePowerUserMode_emits_false_initially() = runTest {
        val useCase = ObservePowerUserModeUseCase(repo)
        assertEquals(false, useCase().first())
    }

    @Test
    fun setPowerUserMode_updates() = runTest {
        val setUseCase = SetPowerUserModeUseCase(repo)
        setUseCase(true)
        assertEquals(true, repo.powerUserMode.value)
    }
}
