package com.riox432.civitdeck.feature.comfyui.di

import com.riox432.civitdeck.data.image.SaveGeneratedImageUseCase
import com.riox432.civitdeck.domain.repository.CivitaiLinkRepository
import com.riox432.civitdeck.domain.repository.ComfyHubRepository
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
import com.riox432.civitdeck.domain.repository.ComfyUIGenerationRepository
import com.riox432.civitdeck.domain.repository.ComfyUIHistoryRepository
import com.riox432.civitdeck.domain.repository.ComfyUIQueueRepository
import com.riox432.civitdeck.domain.repository.SDWebUIAssetRepository
import com.riox432.civitdeck.domain.repository.SDWebUIConnectionRepository
import com.riox432.civitdeck.domain.repository.SDWebUIGenerationRepository
import com.riox432.civitdeck.feature.comfyui.data.repository.CivitaiLinkRepositoryImpl
import com.riox432.civitdeck.feature.comfyui.data.repository.ComfyHubRepositoryImpl
import com.riox432.civitdeck.feature.comfyui.data.repository.ComfyUIConnectionRepositoryImpl
import com.riox432.civitdeck.feature.comfyui.data.repository.ComfyUIGenerationRepositoryImpl
import com.riox432.civitdeck.feature.comfyui.data.repository.ComfyUIHistoryRepositoryImpl
import com.riox432.civitdeck.feature.comfyui.data.repository.ComfyUIQueueRepositoryImpl
import com.riox432.civitdeck.feature.comfyui.data.repository.SDWebUIRepositoryImpl
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ActivateComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ActivateSDWebUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ApplyWorkflowTemplateUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.CancelComfyUIJobUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.CancelLinkActivityUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ConnectCivitaiLinkUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.DeleteComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.DeleteSDWebUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.DeleteWorkflowTemplateUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.DisconnectCivitaiLinkUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ExportWorkflowTemplateUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUICheckpointsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUIControlNetsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUIHistoryItemUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUIHistoryUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUILorasUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchSDWebUIModelsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchSDWebUISamplersUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchSDWebUIVaesUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FindMatchingLocalModelUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.GenerateSDWebUIImageUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.GetComfyHubWorkflowDetailUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.GetWorkflowTemplatesUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ImportComfyHubWorkflowUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ImportWorkflowTemplateUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ImportWorkflowUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.InterruptSDWebUIGenerationUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveActiveComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveActiveSDWebUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveCivitaiLinkActivitiesUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveCivitaiLinkStatusUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveComfyUIConnectionsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveComfyUIQueueUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveGenerationProgressUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveSDWebUIConnectionsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.PollComfyUIResultUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.PopulateGenerationFromModelUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SaveComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SaveSDWebUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SaveWorkflowTemplateUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SearchComfyHubWorkflowsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SendResourceToPCUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SubmitComfyUIGenerationUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.TestComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.TestSDWebUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.plugin.ComfyUIWorkflowPlugin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val comfyuiModule = module {
    single<ComfyUIConnectionRepository> { ComfyUIConnectionRepositoryImpl(get(), get()) }
    single<ComfyUIGenerationRepository> { ComfyUIGenerationRepositoryImpl(get(), get(), get(), get()) }
    single<ComfyUIQueueRepository> { ComfyUIQueueRepositoryImpl(get(), get()) }
    single<ComfyUIHistoryRepository> { ComfyUIHistoryRepositoryImpl(get(), get()) }
    factory { FetchComfyUIHistoryUseCase(get()) }
    factory { FetchComfyUIHistoryItemUseCase(get()) }
    factory { ObserveComfyUIConnectionsUseCase(get()) }
    factory { ObserveActiveComfyUIConnectionUseCase(get()) }
    factory { SaveComfyUIConnectionUseCase(get()) }
    factory { DeleteComfyUIConnectionUseCase(get()) }
    factory { ActivateComfyUIConnectionUseCase(get()) }
    factory { TestComfyUIConnectionUseCase(get()) }
    factory { FetchComfyUICheckpointsUseCase(get()) }
    factory { FetchComfyUILorasUseCase(get()) }
    factory { FetchComfyUIControlNetsUseCase(get()) }
    factory { ImportWorkflowUseCase() }
    factory { SubmitComfyUIGenerationUseCase(get()) }
    factory { PollComfyUIResultUseCase(get()) }
    factory { ObserveGenerationProgressUseCase(get()) }
    factory { ObserveComfyUIQueueUseCase(get()) }
    factory { CancelComfyUIJobUseCase(get()) }
    factory { FindMatchingLocalModelUseCase(get()) }
    factory { PopulateGenerationFromModelUseCase() }
    factory { SaveGeneratedImageUseCase(get(named("comfyui")), get()) }
    // ComfyHub
    single<ComfyHubRepository> { ComfyHubRepositoryImpl(get(), get(), get()) }
    factory { SearchComfyHubWorkflowsUseCase(get()) }
    factory { GetComfyHubWorkflowDetailUseCase(get()) }
    factory { ImportComfyHubWorkflowUseCase(get()) }

    // Workflow template use cases
    factory { GetWorkflowTemplatesUseCase(get()) }
    factory { SaveWorkflowTemplateUseCase(get()) }
    factory { DeleteWorkflowTemplateUseCase(get()) }
    factory { ExportWorkflowTemplateUseCase() }
    factory { ImportWorkflowTemplateUseCase(get()) }
    factory { ApplyWorkflowTemplateUseCase() }

    // SD WebUI
    singleOf(::SDWebUIRepositoryImpl) {
        bind<SDWebUIConnectionRepository>()
        bind<SDWebUIAssetRepository>()
        bind<SDWebUIGenerationRepository>()
    }
    factory { ObserveSDWebUIConnectionsUseCase(get()) }
    factory { ObserveActiveSDWebUIConnectionUseCase(get()) }
    factory { SaveSDWebUIConnectionUseCase(get()) }
    factory { DeleteSDWebUIConnectionUseCase(get()) }
    factory { ActivateSDWebUIConnectionUseCase(get()) }
    factory { TestSDWebUIConnectionUseCase(get()) }
    factory { FetchSDWebUIModelsUseCase(get()) }
    factory { FetchSDWebUISamplersUseCase(get()) }
    factory { FetchSDWebUIVaesUseCase(get()) }
    factory { GenerateSDWebUIImageUseCase(get()) }
    factory { InterruptSDWebUIGenerationUseCase(get()) }

    // Civitai Link
    single<CivitaiLinkRepository> { CivitaiLinkRepositoryImpl(get()) }
    factory { ObserveCivitaiLinkStatusUseCase(get()) }
    factory { ObserveCivitaiLinkActivitiesUseCase(get()) }
    factory { ConnectCivitaiLinkUseCase(get(), get()) }
    factory { DisconnectCivitaiLinkUseCase(get()) }
    factory { SendResourceToPCUseCase(get()) }
    factory { CancelLinkActivityUseCase(get()) }

    // Plugin adapter
    single { ComfyUIWorkflowPlugin(get()) }
}
