package com.riox432.civitdeck.di

import com.riox432.civitdeck.DesktopAppVersionProvider
import com.riox432.civitdeck.domain.repository.AppVersionProvider
import com.riox432.civitdeck.ui.comfyui.ComfyUIGenerationViewModel
import com.riox432.civitdeck.ui.comfyui.ComfyUIHistoryViewModel
import com.riox432.civitdeck.ui.comfyui.ComfyUISettingsViewModel
import com.riox432.civitdeck.ui.comfyui.SDWebUIGenerationViewModel
import com.riox432.civitdeck.ui.comfyui.SDWebUISettingsViewModel
import com.riox432.civitdeck.ui.detail.ModelDetailViewModel
import com.riox432.civitdeck.ui.externalserver.ExternalServerGalleryViewModel
import com.riox432.civitdeck.ui.externalserver.ExternalServerSettingsViewModel
import com.riox432.civitdeck.ui.analytics.DesktopAnalyticsViewModel
import com.riox432.civitdeck.ui.comfyui.template.DesktopWorkflowTemplateViewModel
import com.riox432.civitdeck.ui.downloadqueue.DesktopDownloadQueueViewModel
import com.riox432.civitdeck.ui.notificationcenter.DesktopNotificationCenterViewModel
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
        DesktopSearchViewModel(get(), get(), get(), get(), get())
    }
    viewModel { params ->
        ModelDetailViewModel(
            params.get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel {
        DesktopFeedViewModel(get(), get())
    }
    viewModel {
        DesktopDiscoveryViewModel(get(), get())
    }
    viewModel {
        DesktopAnalyticsViewModel(get())
    }
    // BrowsingHistoryViewModel is now registered in searchModule
    // ComfyUI ViewModels
    viewModel {
        ComfyUISettingsViewModel(get(), get(), get(), get(), get(), get())
    }
    viewModel {
        ComfyUIGenerationViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
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
        ExternalServerGalleryViewModel(get(), get(), get(), get(), get(), get(), get())
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
    // Download Queue ViewModel
    viewModel {
        DesktopDownloadQueueViewModel(get(), get(), get(), get(), get(), get())
    }
    // Workflow Template ViewModel
    viewModel {
        DesktopWorkflowTemplateViewModel(get(), get(), get(), get(), get())
    }
}
