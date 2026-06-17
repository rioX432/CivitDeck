package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.export.DatasetZipWriterFactory
import com.riox432.civitdeck.data.export.DatasetZipWriterFactoryImpl
import com.riox432.civitdeck.data.export.ExportPathProvider
import com.riox432.civitdeck.data.export.ExportPathProviderImpl
import com.riox432.civitdeck.data.local.AndroidNetworkMonitor
import com.riox432.civitdeck.data.local.getDatabaseBuilder
import com.riox432.civitdeck.domain.repository.NetworkRepository
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { getDatabaseBuilder(get()) }
    single<NetworkRepository> { AndroidNetworkMonitor(get()) }
    single<DatasetZipWriterFactory> { DatasetZipWriterFactoryImpl() }
    single<ExportPathProvider> { ExportPathProviderImpl(get()) }
}
