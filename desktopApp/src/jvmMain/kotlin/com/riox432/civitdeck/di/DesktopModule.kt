package com.riox432.civitdeck.di

import com.riox432.civitdeck.DesktopAppVersionProvider
import com.riox432.civitdeck.domain.repository.AppVersionProvider
import com.riox432.civitdeck.ui.detail.ModelDetailViewModel
import com.riox432.civitdeck.ui.analytics.DesktopAnalyticsViewModel
import com.riox432.civitdeck.ui.downloadqueue.DesktopDownloadQueueViewModel
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
    viewModel {
        DesktopBrowsingHistoryViewModel(get(), get(), get())
    }
    // ComfyUI, SDWebUI, ExternalServer VMs now registered in shared modules
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
    // Workflow Template VM now registered in shared comfyuiModule
}
