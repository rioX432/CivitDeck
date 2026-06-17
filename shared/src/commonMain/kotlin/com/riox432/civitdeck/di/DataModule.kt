package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.export.ExportRepositoryImpl
import com.riox432.civitdeck.data.export.KohyaZipExportPlugin
import com.riox432.civitdeck.domain.repository.ExportRepository
import com.riox432.civitdeck.feature.collections.domain.usecase.ExportWithPluginUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.GetAvailableExportFormatsUseCase
import org.koin.dsl.module

val dataModule = module {
    // ImageSaver is registered in comfyuiPlatformModule (interface + platform impl)

    // Export
    single<ExportRepository> { ExportRepositoryImpl(get(), get(), get(), get()) }
    single { KohyaZipExportPlugin(get()) }
    factory { GetAvailableExportFormatsUseCase(get()) }
    factory { ExportWithPluginUseCase(get()) }
}
