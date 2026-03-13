package com.riox432.civitdeck.feature.externalserver.di

import com.riox432.civitdeck.feature.externalserver.data.repository.ExternalServerConfigRepositoryImpl
import com.riox432.civitdeck.feature.externalserver.data.repository.ExternalServerImagesRepositoryImpl
import com.riox432.civitdeck.feature.externalserver.domain.repository.ExternalServerConfigRepository
import com.riox432.civitdeck.feature.externalserver.domain.repository.ExternalServerImagesRepository
import com.riox432.civitdeck.feature.externalserver.domain.usecase.ActivateExternalServerConfigUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.DeleteExternalServerConfigUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.ExecuteGenerationUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetDependentChoicesUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetExternalServerCapabilitiesUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetExternalServerImagesUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetGenerationOptionsUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetGenerationStatusUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.ObserveActiveExternalServerConfigUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.ObserveExternalServerConfigsUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.SaveExternalServerConfigUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.TestExternalServerConnectionUseCase
import com.riox432.civitdeck.feature.externalserver.plugin.ExternalServerWorkflowPlugin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val externalServerModule = module {
    singleOf(::ExternalServerConfigRepositoryImpl) { bind<ExternalServerConfigRepository>() }
    singleOf(::ExternalServerImagesRepositoryImpl) { bind<ExternalServerImagesRepository>() }

    factory { ObserveExternalServerConfigsUseCase(get()) }
    factory { ObserveActiveExternalServerConfigUseCase(get()) }
    factory { SaveExternalServerConfigUseCase(get()) }
    factory { DeleteExternalServerConfigUseCase(get()) }
    factory { ActivateExternalServerConfigUseCase(get()) }
    factory { TestExternalServerConnectionUseCase(get(), get()) }
    factory { GetExternalServerCapabilitiesUseCase(get()) }
    factory { GetExternalServerImagesUseCase(get()) }
    factory { GetGenerationOptionsUseCase(get()) }
    factory { GetDependentChoicesUseCase(get()) }
    factory { ExecuteGenerationUseCase(get()) }
    factory { GetGenerationStatusUseCase(get()) }

    // Plugin adapter
    single { ExternalServerWorkflowPlugin(get(), get()) }
}
