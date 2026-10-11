package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.api.DATASET_MEDIA_CLIENT
import com.riox432.civitdeck.data.api.createDatasetMediaHttpClient
import com.riox432.civitdeck.data.export.ExportRepositoryImpl
import com.riox432.civitdeck.data.export.KohyaZipExportPlugin
import com.riox432.civitdeck.domain.repository.ExportRepository
import com.riox432.civitdeck.feature.collections.domain.usecase.ExportWithPluginUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.GetAvailableExportFormatsUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

val dataModule = module {
    // ImageSaver is registered in comfyuiPlatformModule (interface + platform impl)

    // Export
    single(named(DATASET_MEDIA_CLIENT)) { createDatasetMediaHttpClient() }
    single<ExportRepository> { ExportRepositoryImpl(get(), get(named(DATASET_MEDIA_CLIENT)), get(), get()) }
    single { KohyaZipExportPlugin(get()) }
    factory { GetAvailableExportFormatsUseCase(get()) }
    factory { ExportWithPluginUseCase(get()) }
}
