package com.riox432.civitdeck.di

import com.riox432.civitdeck.DesktopAppVersionProvider
import com.riox432.civitdeck.domain.repository.AppVersionProvider
import com.riox432.civitdeck.feature.collections.presentation.CollectionDetailViewModel
import com.riox432.civitdeck.feature.collections.presentation.CollectionsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUISettingsViewModel
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerGalleryViewModel
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.AppBehaviorSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.ContentFilterSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.StorageSettingsViewModel
import com.riox432.civitdeck.ui.analytics.DesktopAnalyticsViewModel
import com.riox432.civitdeck.ui.notificationcenter.DesktopNotificationCenterViewModel
import com.riox432.civitdeck.ui.history.DesktopBrowsingHistoryViewModel
import com.riox432.civitdeck.ui.backup.DesktopBackupViewModel
import com.riox432.civitdeck.ui.dataset.DesktopDatasetDetailViewModel
import com.riox432.civitdeck.ui.dataset.DesktopDatasetListViewModel
import com.riox432.civitdeck.ui.discovery.DesktopDiscoveryViewModel
import com.riox432.civitdeck.ui.feed.DesktopFeedViewModel
import com.riox432.civitdeck.ui.plugin.DesktopPluginViewModel
import com.riox432.civitdeck.ui.search.DesktopSearchViewModel
import com.riox432.civitdeck.ui.update.DesktopUpdateViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val desktopModule = module {
    single<AppVersionProvider> { DesktopAppVersionProvider() }
    viewModel { DesktopUpdateViewModel(get(), get(), get()) }
    viewModel {
        DesktopSearchViewModel(get(), get(), get(), get())
    }
    viewModel { params ->
        ModelDetailViewModel(
            params.get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel {
        CollectionsViewModel(get(), get(), get(), get())
    }
    viewModel { params ->
        CollectionDetailViewModel(params.get(), get(), get(), get(), get())
    }
    viewModel {
        DesktopFeedViewModel(get(), get())
    }
    viewModel {
        DesktopDiscoveryViewModel(get())
    }
    viewModel {
        DesktopAnalyticsViewModel(get())
    }
    viewModel {
        DesktopBrowsingHistoryViewModel(get(), get(), get())
    }
    // Settings ViewModels
    viewModel {
        DisplaySettingsViewModel(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel {
        ContentFilterSettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
    viewModel {
        AppBehaviorSettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get())
    }
    viewModel {
        StorageSettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
    // ComfyUI ViewModels
    viewModel {
        ComfyUISettingsViewModel(get(), get(), get(), get(), get(), get())
    }
    viewModel {
        ComfyUIGenerationViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
    viewModel {
        ComfyUIHistoryViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
    // SD WebUI ViewModels
    viewModel {
        SDWebUISettingsViewModel(get(), get(), get(), get(), get(), get())
    }
    viewModel {
        SDWebUIGenerationViewModel(get(), get(), get(), get(), get())
    }
    // External Server ViewModels
    viewModel {
        ExternalServerSettingsViewModel(get(), get(), get(), get(), get(), get())
    }
    viewModel {
        ExternalServerGalleryViewModel(get(), get(), get(), get(), get(), get())
    }
    // Dataset ViewModels
    viewModel {
        DesktopDatasetListViewModel(get(), get(), get(), get())
    }
    viewModel { params ->
        DesktopDatasetDetailViewModel(params.get(), get(), get(), get(), get(), get(), get())
    }
    // Backup ViewModel
    viewModel {
        DesktopBackupViewModel(get(), get(), get())
    }
    // Plugin ViewModel
    viewModel {
        DesktopPluginViewModel(get(), get(), get(), get(), get(), get())
    }
    // Notification Center ViewModel
    viewModel {
        DesktopNotificationCenterViewModel(get(), get(), get())
    }
}
