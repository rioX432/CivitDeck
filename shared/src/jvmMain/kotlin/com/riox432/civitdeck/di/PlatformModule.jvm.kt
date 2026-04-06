package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.local.JvmNetworkMonitor
import com.riox432.civitdeck.data.local.getDatabaseBuilder
import com.riox432.civitdeck.domain.download.DownloadScheduler
import com.riox432.civitdeck.domain.repository.NetworkRepository
import com.riox432.civitdeck.download.DesktopDownloadScheduler
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { getDatabaseBuilder() }
    single<NetworkRepository> { JvmNetworkMonitor() }
    single<DownloadScheduler> { DesktopDownloadScheduler() }
}
