package com.riox432.civitdeck.di

import com.riox432.civitdeck.DesktopAppVersionProvider
import com.riox432.civitdeck.domain.repository.AppVersionProvider
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import com.riox432.civitdeck.ui.discovery.DesktopDiscoveryViewModel
import com.riox432.civitdeck.ui.update.DesktopUpdateViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val desktopModule = module {
    single<AppVersionProvider> { DesktopAppVersionProvider() }
    viewModel { DesktopUpdateViewModel(get(), get(), get()) }
    viewModel { params ->
        ModelDetailViewModel(
            params.get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel {
        DesktopDiscoveryViewModel(get(), get())
    }
    // BrowsingHistoryViewModel now registered in shared searchModule
    // ComfyUI, SDWebUI, ExternalServer VMs now registered in shared modules
    // Feed, Analytics, DatasetList, DatasetDetail, Backup, Plugin, NotificationCenter,
    // DownloadQueue VMs now registered in shared Phase3ViewModelModule
    // Workflow Template VM now registered in shared comfyuiModule
}
