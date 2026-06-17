package com.riox432.civitdeck.testing

import com.riox432.civitdeck.domain.model.NavShortcut
import com.riox432.civitdeck.domain.model.PollingInterval
import com.riox432.civitdeck.domain.repository.AppBehaviorPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [AppBehaviorPreferencesRepository] for ViewModel tests.
 */
class FakeAppBehaviorPreferencesRepository(
    powerUserMode: Boolean = false,
    feedQualityThreshold: Int = 0,
) : AppBehaviorPreferencesRepository {

    val powerUserModeFlow = MutableStateFlow(powerUserMode)
    val notificationsEnabledFlow = MutableStateFlow(false)
    val generationNotificationsEnabledFlow = MutableStateFlow(false)
    val pollingIntervalFlow = MutableStateFlow(PollingInterval.Off)
    val seenTutorialVersionFlow = MutableStateFlow(0)
    val customNavShortcutsFlow = MutableStateFlow(emptyList<NavShortcut>())
    val feedQualityThresholdFlow = MutableStateFlow(feedQualityThreshold)

    override fun observePowerUserMode(): Flow<Boolean> = powerUserModeFlow
    override suspend fun setPowerUserMode(enabled: Boolean) {
        powerUserModeFlow.value = enabled
    }

    override fun observeNotificationsEnabled(): Flow<Boolean> = notificationsEnabledFlow
    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabledFlow.value = enabled
    }

    override fun observePollingInterval(): Flow<PollingInterval> = pollingIntervalFlow
    override suspend fun setPollingInterval(interval: PollingInterval) {
        pollingIntervalFlow.value = interval
    }

    override fun observeSeenTutorialVersion(): Flow<Int> = seenTutorialVersionFlow
    override suspend fun setSeenTutorialVersion(version: Int) {
        seenTutorialVersionFlow.value = version
    }

    override fun observeCustomNavShortcuts(): Flow<List<NavShortcut>> = customNavShortcutsFlow
    override suspend fun setCustomNavShortcuts(items: List<NavShortcut>) {
        customNavShortcutsFlow.value = items
    }

    override fun observeFeedQualityThreshold(): Flow<Int> = feedQualityThresholdFlow
    override suspend fun setFeedQualityThreshold(threshold: Int) {
        feedQualityThresholdFlow.value = threshold
    }

    override fun observeGenerationNotificationsEnabled(): Flow<Boolean> =
        generationNotificationsEnabledFlow
    override suspend fun setGenerationNotificationsEnabled(enabled: Boolean) {
        generationNotificationsEnabledFlow.value = enabled
    }
}
