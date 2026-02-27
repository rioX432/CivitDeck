package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.local.IosNetworkMonitor
import com.riox432.civitdeck.data.local.NetworkMonitor
import com.riox432.civitdeck.data.local.getDatabaseBuilder
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { getDatabaseBuilder() }
    single<NetworkMonitor> { IosNetworkMonitor() }
}
