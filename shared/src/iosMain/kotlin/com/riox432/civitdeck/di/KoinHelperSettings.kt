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
import com.riox432.civitdeck.domain.usecase.ObserveFrontDoorModeUseCase
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
import com.riox432.civitdeck.domain.usecase.SavePromptUseCase
import com.riox432.civitdeck.domain.usecase.SetAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.SetAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.SetApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.SetCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.SetCustomNavShortcutsUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.SetFrontDoorModeUseCase
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
import com.riox432.civitdeck.plugin.usecase.ActivateThemePluginUseCase
import com.riox432.civitdeck.plugin.usecase.GetActiveThemeUseCase
import com.riox432.civitdeck.plugin.usecase.ImportThemeUseCase
import com.riox432.civitdeck.plugin.usecase.ObserveThemePluginsUseCase

// Theme & Display Settings
fun KoinHelper.getObserveAccentColorUseCase(): ObserveAccentColorUseCase = resolve()
fun KoinHelper.getSetAccentColorUseCase(): SetAccentColorUseCase = resolve()
fun KoinHelper.getObserveAmoledDarkModeUseCase(): ObserveAmoledDarkModeUseCase = resolve()
fun KoinHelper.getSetAmoledDarkModeUseCase(): SetAmoledDarkModeUseCase = resolve()
fun KoinHelper.getObserveThemeModeUseCase(): ObserveThemeModeUseCase = resolve()
fun KoinHelper.getSetThemeModeUseCase(): SetThemeModeUseCase = resolve()
fun KoinHelper.getObserveDefaultSortOrderUseCase(): ObserveDefaultSortOrderUseCase = resolve()
fun KoinHelper.getSetDefaultSortOrderUseCase(): SetDefaultSortOrderUseCase = resolve()
fun KoinHelper.getObserveDefaultTimePeriodUseCase(): ObserveDefaultTimePeriodUseCase = resolve()
fun KoinHelper.getSetDefaultTimePeriodUseCase(): SetDefaultTimePeriodUseCase = resolve()
fun KoinHelper.getObserveGridColumnsUseCase(): ObserveGridColumnsUseCase = resolve()
fun KoinHelper.getSetGridColumnsUseCase(): SetGridColumnsUseCase = resolve()
fun KoinHelper.getObserveQualityThresholdUseCase(): ObserveQualityThresholdUseCase = resolve()
fun KoinHelper.getSetQualityThresholdUseCase(): SetQualityThresholdUseCase = resolve()
fun KoinHelper.getObservePowerUserModeUseCase(): ObservePowerUserModeUseCase = resolve()
fun KoinHelper.getSetPowerUserModeUseCase(): SetPowerUserModeUseCase = resolve()
fun KoinHelper.getObserveCustomNavShortcutsUseCase(): ObserveCustomNavShortcutsUseCase = resolve()
fun KoinHelper.getSetCustomNavShortcutsUseCase(): SetCustomNavShortcutsUseCase = resolve()
fun KoinHelper.getObserveSeenTutorialVersionUseCase(): ObserveSeenTutorialVersionUseCase = resolve()
fun KoinHelper.getSetSeenTutorialVersionUseCase(): SetSeenTutorialVersionUseCase = resolve()

// NSFW & Content Filtering
fun KoinHelper.getObserveNsfwFilterUseCase(): ObserveNsfwFilterUseCase = resolve()
fun KoinHelper.getSetNsfwFilterUseCase(): SetNsfwFilterUseCase = resolve()
fun KoinHelper.getObserveNsfwBlurSettingsUseCase(): ObserveNsfwBlurSettingsUseCase = resolve()
fun KoinHelper.getSetNsfwBlurSettingsUseCase(): SetNsfwBlurSettingsUseCase = resolve()

// Front Door (web/share link host: civitai.com / civitai.red)
fun KoinHelper.getObserveFrontDoorModeUseCase(): ObserveFrontDoorModeUseCase = resolve()
fun KoinHelper.getSetFrontDoorModeUseCase(): SetFrontDoorModeUseCase = resolve()

// Authentication & API Key
fun KoinHelper.getObserveApiKeyUseCase(): ObserveApiKeyUseCase = resolve()
fun KoinHelper.getSetApiKeyUseCase(): SetApiKeyUseCase = resolve()
fun KoinHelper.getValidateApiKeyUseCase(): ValidateApiKeyUseCase = resolve()
fun KoinHelper.getApiKeyProvider(): ApiKeyProvider = resolve()

// Notifications & Model Updates
fun KoinHelper.getCheckModelUpdatesUseCase(): CheckModelUpdatesUseCase = resolve()
fun KoinHelper.getObserveNotificationsEnabledUseCase(): ObserveNotificationsEnabledUseCase = resolve()
fun KoinHelper.getSetNotificationsEnabledUseCase(): SetNotificationsEnabledUseCase = resolve()
fun KoinHelper.getObservePollingIntervalUseCase(): ObservePollingIntervalUseCase = resolve()
fun KoinHelper.getSetPollingIntervalUseCase(): SetPollingIntervalUseCase = resolve()
fun KoinHelper.getModelUpdateNotificationsUseCase(): GetModelUpdateNotificationsUseCase = resolve()
fun KoinHelper.getUnreadNotificationCountUseCase(): GetUnreadNotificationCountUseCase = resolve()
fun KoinHelper.getMarkNotificationReadUseCase(): MarkNotificationReadUseCase = resolve()
fun KoinHelper.getMarkAllNotificationsReadUseCase(): MarkAllNotificationsReadUseCase = resolve()
fun KoinHelper.getCheckAndStoreModelUpdatesUseCase(): CheckAndStoreModelUpdatesUseCase = resolve()

// Offline Cache & Network
fun KoinHelper.getObserveNetworkStatusUseCase(): ObserveNetworkStatusUseCase = resolve()
fun KoinHelper.getCacheInfoUseCase(): GetCacheInfoUseCase = resolve()
fun KoinHelper.getEvictCacheUseCase(): EvictCacheUseCase = resolve()
fun KoinHelper.getClearCacheUseCase(): ClearCacheUseCase = resolve()
fun KoinHelper.getObserveOfflineCacheEnabledUseCase(): ObserveOfflineCacheEnabledUseCase = resolve()
fun KoinHelper.getSetOfflineCacheEnabledUseCase(): SetOfflineCacheEnabledUseCase = resolve()
fun KoinHelper.getObserveCacheSizeLimitUseCase(): ObserveCacheSizeLimitUseCase = resolve()
fun KoinHelper.getSetCacheSizeLimitUseCase(): SetCacheSizeLimitUseCase = resolve()

// Prompts & Templates
fun KoinHelper.getAutoSavePromptUseCase(): AutoSavePromptUseCase = resolve()
fun KoinHelper.getToggleTemplateUseCase(): ToggleTemplateUseCase = resolve()
fun KoinHelper.getSearchSavedPromptsUseCase(): SearchSavedPromptsUseCase = resolve()
fun KoinHelper.getObserveTemplatesUseCase(): ObserveTemplatesUseCase = resolve()
fun KoinHelper.getSavePromptUseCase(): SavePromptUseCase = resolve()
fun KoinHelper.getObserveSavedPromptsUseCase(): ObserveSavedPromptsUseCase = resolve()
fun KoinHelper.getDeleteSavedPromptUseCase(): DeleteSavedPromptUseCase = resolve()

// Backup & Restore
fun KoinHelper.getCreateBackupUseCase(): CreateBackupUseCase = resolve()
fun KoinHelper.getRestoreBackupUseCase(): RestoreBackupUseCase = resolve()
fun KoinHelper.getParseBackupUseCase(): ParseBackupUseCase = resolve()

// Plugin Management
fun KoinHelper.getInstallPluginUseCase(): InstallPluginUseCase = resolve()
fun KoinHelper.getUninstallPluginUseCase(): UninstallPluginUseCase = resolve()
fun KoinHelper.getActivatePluginUseCase(): ActivatePluginUseCase = resolve()
fun KoinHelper.getDeactivatePluginUseCase(): DeactivatePluginUseCase = resolve()
fun KoinHelper.getObserveInstalledPluginsUseCase(): ObserveInstalledPluginsUseCase = resolve()
fun KoinHelper.getGetPluginConfigUseCase(): GetPluginConfigUseCase = resolve()
fun KoinHelper.getUpdatePluginConfigUseCase(): UpdatePluginConfigUseCase = resolve()

// Theme Plugins
fun KoinHelper.getImportThemeUseCase(): ImportThemeUseCase = resolve()
fun KoinHelper.getGetActiveThemeUseCase(): GetActiveThemeUseCase = resolve()
fun KoinHelper.getObserveThemePluginsUseCase(): ObserveThemePluginsUseCase = resolve()
fun KoinHelper.getActivateThemePluginUseCase(): ActivateThemePluginUseCase = resolve()

// Share Hashtags
fun KoinHelper.getObserveShareHashtagsUseCase(): ObserveShareHashtagsUseCase = resolve()
fun KoinHelper.getAddShareHashtagUseCase(): AddShareHashtagUseCase = resolve()
fun KoinHelper.getRemoveShareHashtagUseCase(): RemoveShareHashtagUseCase = resolve()
fun KoinHelper.getToggleShareHashtagUseCase(): ToggleShareHashtagUseCase = resolve()
