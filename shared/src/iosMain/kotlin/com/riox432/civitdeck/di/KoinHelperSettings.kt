@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.api.ApiKeyProvider
import com.riox432.civitdeck.domain.usecase.ActivatePluginUseCase
import com.riox432.civitdeck.domain.usecase.AddShareHashtagUseCase
import com.riox432.civitdeck.domain.usecase.AutoSavePromptUseCase
import com.riox432.civitdeck.domain.usecase.CheckAndStoreModelUpdatesUseCase
import com.riox432.civitdeck.domain.usecase.CheckModelUpdatesUseCase
import com.riox432.civitdeck.domain.usecase.ClearCacheUseCase
import com.riox432.civitdeck.domain.usecase.CreateBackupUseCase
import com.riox432.civitdeck.domain.usecase.DeactivatePluginUseCase
import com.riox432.civitdeck.domain.usecase.EvictCacheUseCase
import com.riox432.civitdeck.domain.usecase.GetCacheInfoUseCase
import com.riox432.civitdeck.domain.usecase.GetModelUpdateNotificationsUseCase
import com.riox432.civitdeck.domain.usecase.GetPluginConfigUseCase
import com.riox432.civitdeck.domain.usecase.GetUnreadNotificationCountUseCase
import com.riox432.civitdeck.domain.usecase.InstallPluginUseCase
import com.riox432.civitdeck.domain.usecase.MarkAllNotificationsReadUseCase
import com.riox432.civitdeck.domain.usecase.MarkNotificationReadUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.ObserveApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCustomNavShortcutsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveInstalledPluginsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNetworkStatusUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwBlurSettingsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObserveOfflineCacheEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObservePollingIntervalUseCase
import com.riox432.civitdeck.domain.usecase.ObservePowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.ObserveQualityThresholdUseCase
import com.riox432.civitdeck.domain.usecase.ObserveSeenTutorialVersionUseCase
import com.riox432.civitdeck.domain.usecase.ObserveShareHashtagsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveThemeModeUseCase
import com.riox432.civitdeck.domain.usecase.ParseBackupUseCase
import com.riox432.civitdeck.domain.usecase.RemoveShareHashtagUseCase
import com.riox432.civitdeck.domain.usecase.RestoreBackupUseCase
import com.riox432.civitdeck.domain.usecase.SetAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.SetAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.SetApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.SetCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.SetCustomNavShortcutsUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.SetGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.SetNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.SetNsfwBlurSettingsUseCase
import com.riox432.civitdeck.domain.usecase.SetNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.SetOfflineCacheEnabledUseCase
import com.riox432.civitdeck.domain.usecase.SetPollingIntervalUseCase
import com.riox432.civitdeck.domain.usecase.SetPowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.SetQualityThresholdUseCase
import com.riox432.civitdeck.domain.usecase.SetSeenTutorialVersionUseCase
import com.riox432.civitdeck.domain.usecase.SetThemeModeUseCase
import com.riox432.civitdeck.domain.usecase.ToggleShareHashtagUseCase
import com.riox432.civitdeck.domain.usecase.UninstallPluginUseCase
import com.riox432.civitdeck.domain.usecase.UpdatePluginConfigUseCase
import com.riox432.civitdeck.domain.usecase.ValidateApiKeyUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.DeleteSavedPromptUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ObserveSavedPromptsUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ObserveTemplatesUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.SearchSavedPromptsUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ToggleTemplateUseCase
import com.riox432.civitdeck.usecase.ActivateThemePluginUseCase
import com.riox432.civitdeck.usecase.GetActiveThemeUseCase
import com.riox432.civitdeck.usecase.ImportThemeUseCase
import com.riox432.civitdeck.usecase.ObserveThemePluginsUseCase
import org.koin.mp.KoinPlatform.getKoin

// Theme & Display Settings
fun KoinHelper.getObserveAccentColorUseCase(): ObserveAccentColorUseCase = getKoin().get()
fun KoinHelper.getSetAccentColorUseCase(): SetAccentColorUseCase = getKoin().get()
fun KoinHelper.getObserveAmoledDarkModeUseCase(): ObserveAmoledDarkModeUseCase = getKoin().get()
fun KoinHelper.getSetAmoledDarkModeUseCase(): SetAmoledDarkModeUseCase = getKoin().get()
fun KoinHelper.getObserveThemeModeUseCase(): ObserveThemeModeUseCase = getKoin().get()
fun KoinHelper.getSetThemeModeUseCase(): SetThemeModeUseCase = getKoin().get()
fun KoinHelper.getObserveDefaultSortOrderUseCase(): ObserveDefaultSortOrderUseCase = getKoin().get()
fun KoinHelper.getSetDefaultSortOrderUseCase(): SetDefaultSortOrderUseCase = getKoin().get()
fun KoinHelper.getObserveDefaultTimePeriodUseCase(): ObserveDefaultTimePeriodUseCase = getKoin().get()
fun KoinHelper.getSetDefaultTimePeriodUseCase(): SetDefaultTimePeriodUseCase = getKoin().get()
fun KoinHelper.getObserveGridColumnsUseCase(): ObserveGridColumnsUseCase = getKoin().get()
fun KoinHelper.getSetGridColumnsUseCase(): SetGridColumnsUseCase = getKoin().get()
fun KoinHelper.getObserveQualityThresholdUseCase(): ObserveQualityThresholdUseCase = getKoin().get()
fun KoinHelper.getSetQualityThresholdUseCase(): SetQualityThresholdUseCase = getKoin().get()
fun KoinHelper.getObservePowerUserModeUseCase(): ObservePowerUserModeUseCase = getKoin().get()
fun KoinHelper.getSetPowerUserModeUseCase(): SetPowerUserModeUseCase = getKoin().get()
fun KoinHelper.getObserveCustomNavShortcutsUseCase(): ObserveCustomNavShortcutsUseCase = getKoin().get()
fun KoinHelper.getSetCustomNavShortcutsUseCase(): SetCustomNavShortcutsUseCase = getKoin().get()
fun KoinHelper.getObserveSeenTutorialVersionUseCase(): ObserveSeenTutorialVersionUseCase = getKoin().get()
fun KoinHelper.getSetSeenTutorialVersionUseCase(): SetSeenTutorialVersionUseCase = getKoin().get()

// NSFW & Content Filtering
fun KoinHelper.getObserveNsfwFilterUseCase(): ObserveNsfwFilterUseCase = getKoin().get()
fun KoinHelper.getSetNsfwFilterUseCase(): SetNsfwFilterUseCase = getKoin().get()
fun KoinHelper.getObserveNsfwBlurSettingsUseCase(): ObserveNsfwBlurSettingsUseCase = getKoin().get()
fun KoinHelper.getSetNsfwBlurSettingsUseCase(): SetNsfwBlurSettingsUseCase = getKoin().get()

// Authentication & API Key
fun KoinHelper.getObserveApiKeyUseCase(): ObserveApiKeyUseCase = getKoin().get()
fun KoinHelper.getSetApiKeyUseCase(): SetApiKeyUseCase = getKoin().get()
fun KoinHelper.getValidateApiKeyUseCase(): ValidateApiKeyUseCase = getKoin().get()
fun KoinHelper.getApiKeyProvider(): ApiKeyProvider = getKoin().get()

// Notifications & Model Updates
fun KoinHelper.getCheckModelUpdatesUseCase(): CheckModelUpdatesUseCase = getKoin().get()
fun KoinHelper.getObserveNotificationsEnabledUseCase(): ObserveNotificationsEnabledUseCase = getKoin().get()
fun KoinHelper.getSetNotificationsEnabledUseCase(): SetNotificationsEnabledUseCase = getKoin().get()
fun KoinHelper.getObservePollingIntervalUseCase(): ObservePollingIntervalUseCase = getKoin().get()
fun KoinHelper.getSetPollingIntervalUseCase(): SetPollingIntervalUseCase = getKoin().get()
fun KoinHelper.getModelUpdateNotificationsUseCase(): GetModelUpdateNotificationsUseCase = getKoin().get()
fun KoinHelper.getUnreadNotificationCountUseCase(): GetUnreadNotificationCountUseCase = getKoin().get()
fun KoinHelper.getMarkNotificationReadUseCase(): MarkNotificationReadUseCase = getKoin().get()
fun KoinHelper.getMarkAllNotificationsReadUseCase(): MarkAllNotificationsReadUseCase = getKoin().get()
fun KoinHelper.getCheckAndStoreModelUpdatesUseCase(): CheckAndStoreModelUpdatesUseCase = getKoin().get()

// Offline Cache & Network
fun KoinHelper.getObserveNetworkStatusUseCase(): ObserveNetworkStatusUseCase = getKoin().get()
fun KoinHelper.getCacheInfoUseCase(): GetCacheInfoUseCase = getKoin().get()
fun KoinHelper.getEvictCacheUseCase(): EvictCacheUseCase = getKoin().get()
fun KoinHelper.getClearCacheUseCase(): ClearCacheUseCase = getKoin().get()
fun KoinHelper.getObserveOfflineCacheEnabledUseCase(): ObserveOfflineCacheEnabledUseCase = getKoin().get()
fun KoinHelper.getSetOfflineCacheEnabledUseCase(): SetOfflineCacheEnabledUseCase = getKoin().get()
fun KoinHelper.getObserveCacheSizeLimitUseCase(): ObserveCacheSizeLimitUseCase = getKoin().get()
fun KoinHelper.getSetCacheSizeLimitUseCase(): SetCacheSizeLimitUseCase = getKoin().get()

// Prompts & Templates
fun KoinHelper.getAutoSavePromptUseCase(): AutoSavePromptUseCase = getKoin().get()
fun KoinHelper.getToggleTemplateUseCase(): ToggleTemplateUseCase = getKoin().get()
fun KoinHelper.getSearchSavedPromptsUseCase(): SearchSavedPromptsUseCase = getKoin().get()
fun KoinHelper.getObserveTemplatesUseCase(): ObserveTemplatesUseCase = getKoin().get()
fun KoinHelper.getSavePromptUseCase(): com.riox432.civitdeck.domain.usecase.SavePromptUseCase = getKoin().get()
fun KoinHelper.getObserveSavedPromptsUseCase(): ObserveSavedPromptsUseCase = getKoin().get()
fun KoinHelper.getDeleteSavedPromptUseCase(): DeleteSavedPromptUseCase = getKoin().get()

// Backup & Restore
fun KoinHelper.getCreateBackupUseCase(): CreateBackupUseCase = getKoin().get()
fun KoinHelper.getRestoreBackupUseCase(): RestoreBackupUseCase = getKoin().get()
fun KoinHelper.getParseBackupUseCase(): ParseBackupUseCase = getKoin().get()

// Plugin Management
fun KoinHelper.getInstallPluginUseCase(): InstallPluginUseCase = getKoin().get()
fun KoinHelper.getUninstallPluginUseCase(): UninstallPluginUseCase = getKoin().get()
fun KoinHelper.getActivatePluginUseCase(): ActivatePluginUseCase = getKoin().get()
fun KoinHelper.getDeactivatePluginUseCase(): DeactivatePluginUseCase = getKoin().get()
fun KoinHelper.getObserveInstalledPluginsUseCase(): ObserveInstalledPluginsUseCase = getKoin().get()
fun KoinHelper.getGetPluginConfigUseCase(): GetPluginConfigUseCase = getKoin().get()
fun KoinHelper.getUpdatePluginConfigUseCase(): UpdatePluginConfigUseCase = getKoin().get()

// Theme Plugins
fun KoinHelper.getImportThemeUseCase(): ImportThemeUseCase = getKoin().get()
fun KoinHelper.getGetActiveThemeUseCase(): GetActiveThemeUseCase = getKoin().get()
fun KoinHelper.getObserveThemePluginsUseCase(): ObserveThemePluginsUseCase = getKoin().get()
fun KoinHelper.getActivateThemePluginUseCase(): ActivateThemePluginUseCase = getKoin().get()

// Share Hashtags
fun KoinHelper.getObserveShareHashtagsUseCase(): ObserveShareHashtagsUseCase = getKoin().get()
fun KoinHelper.getAddShareHashtagUseCase(): AddShareHashtagUseCase = getKoin().get()
fun KoinHelper.getRemoveShareHashtagUseCase(): RemoveShareHashtagUseCase = getKoin().get()
fun KoinHelper.getToggleShareHashtagUseCase(): ToggleShareHashtagUseCase = getKoin().get()
