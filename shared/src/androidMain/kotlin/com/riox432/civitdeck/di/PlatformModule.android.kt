package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.local.AndroidNetworkMonitor
import com.riox432.civitdeck.data.local.getDatabaseBuilder
import com.riox432.civitdeck.domain.repository.NetworkRepository
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { getDatabaseBuilder(get()) }
    single<NetworkRepository> { AndroidNetworkMonitor(get()) }
}
