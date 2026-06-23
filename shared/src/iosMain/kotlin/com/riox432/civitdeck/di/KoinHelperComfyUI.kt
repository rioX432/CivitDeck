// iOS Koin factory bridge: many cohesive single-line ViewModel accessors for Swift interop.
@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.image.SaveGeneratedImageUseCase
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
import com.riox432.civitdeck.domain.usecase.ObserveCivitaiLinkKeyUseCase
import com.riox432.civitdeck.domain.usecase.SetCivitaiLinkKeyUseCase
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
import com.riox432.civitdeck.feature.comfyui.domain.usecase.InterruptComfyUIGenerationUseCase
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
import com.riox432.civitdeck.feature.externalserver.domain.usecase.ActivateExternalServerConfigUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.DeleteExternalServerConfigUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.DeleteServerImagesUseCase
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

// ComfyUI
fun KoinHelper.getObserveComfyUIConnectionsUseCase(): ObserveComfyUIConnectionsUseCase = resolve()
fun KoinHelper.getObserveActiveComfyUIConnectionUseCase(): ObserveActiveComfyUIConnectionUseCase = resolve()
fun KoinHelper.getSaveComfyUIConnectionUseCase(): SaveComfyUIConnectionUseCase = resolve()
fun KoinHelper.getDeleteComfyUIConnectionUseCase(): DeleteComfyUIConnectionUseCase = resolve()
fun KoinHelper.getActivateComfyUIConnectionUseCase(): ActivateComfyUIConnectionUseCase = resolve()
fun KoinHelper.getTestComfyUIConnectionUseCase(): TestComfyUIConnectionUseCase = resolve()
fun KoinHelper.getFetchComfyUICheckpointsUseCase(): FetchComfyUICheckpointsUseCase = resolve()
fun KoinHelper.getFetchComfyUILorasUseCase(): FetchComfyUILorasUseCase = resolve()
fun KoinHelper.getFetchComfyUIControlNetsUseCase(): FetchComfyUIControlNetsUseCase = resolve()
fun KoinHelper.getImportWorkflowUseCase(): ImportWorkflowUseCase = resolve()
fun KoinHelper.getSubmitComfyUIGenerationUseCase(): SubmitComfyUIGenerationUseCase = resolve()
fun KoinHelper.getPollComfyUIResultUseCase(): PollComfyUIResultUseCase = resolve()
fun KoinHelper.getObserveGenerationProgressUseCase(): ObserveGenerationProgressUseCase = resolve()
fun KoinHelper.getObserveComfyUIQueueUseCase(): ObserveComfyUIQueueUseCase = resolve()
fun KoinHelper.getCancelComfyUIJobUseCase(): CancelComfyUIJobUseCase = resolve()
fun KoinHelper.getInterruptComfyUIGenerationUseCase(): InterruptComfyUIGenerationUseCase = resolve()
fun KoinHelper.getComfyUIConnectionRepository(): ComfyUIConnectionRepository = resolve()
fun KoinHelper.getFindMatchingLocalModelUseCase(): FindMatchingLocalModelUseCase = resolve()
fun KoinHelper.getPopulateGenerationFromModelUseCase(): PopulateGenerationFromModelUseCase = resolve()
fun KoinHelper.getSaveGeneratedImageUseCase(): SaveGeneratedImageUseCase = resolve()
fun KoinHelper.getFetchComfyUIHistoryUseCase(): FetchComfyUIHistoryUseCase = resolve()
fun KoinHelper.getFetchComfyUIHistoryItemUseCase(): FetchComfyUIHistoryItemUseCase = resolve()

// ComfyUI Workflow Templates
fun KoinHelper.getGetWorkflowTemplatesUseCase(): GetWorkflowTemplatesUseCase = resolve()
fun KoinHelper.getSaveWorkflowTemplateUseCase(): SaveWorkflowTemplateUseCase = resolve()
fun KoinHelper.getDeleteWorkflowTemplateUseCase(): DeleteWorkflowTemplateUseCase = resolve()
fun KoinHelper.getExportWorkflowTemplateUseCase(): ExportWorkflowTemplateUseCase = resolve()
fun KoinHelper.getImportWorkflowTemplateUseCase(): ImportWorkflowTemplateUseCase = resolve()
fun KoinHelper.getApplyWorkflowTemplateUseCase(): ApplyWorkflowTemplateUseCase = resolve()

// ComfyHub
fun KoinHelper.getSearchComfyHubWorkflowsUseCase(): SearchComfyHubWorkflowsUseCase = resolve()
fun KoinHelper.getGetComfyHubWorkflowDetailUseCase(): GetComfyHubWorkflowDetailUseCase = resolve()
fun KoinHelper.getImportComfyHubWorkflowUseCase(): ImportComfyHubWorkflowUseCase = resolve()

// Civitai Link
fun KoinHelper.getObserveCivitaiLinkKeyUseCase(): ObserveCivitaiLinkKeyUseCase = resolve()
fun KoinHelper.getSetCivitaiLinkKeyUseCase(): SetCivitaiLinkKeyUseCase = resolve()
fun KoinHelper.getObserveCivitaiLinkStatusUseCase(): ObserveCivitaiLinkStatusUseCase = resolve()
fun KoinHelper.getObserveCivitaiLinkActivitiesUseCase(): ObserveCivitaiLinkActivitiesUseCase = resolve()
fun KoinHelper.getConnectCivitaiLinkUseCase(): ConnectCivitaiLinkUseCase = resolve()
fun KoinHelper.getDisconnectCivitaiLinkUseCase(): DisconnectCivitaiLinkUseCase = resolve()
fun KoinHelper.getSendResourceToPCUseCase(): SendResourceToPCUseCase = resolve()
fun KoinHelper.getCancelLinkActivityUseCase(): CancelLinkActivityUseCase = resolve()

// SD WebUI
fun KoinHelper.getObserveSDWebUIConnectionsUseCase(): ObserveSDWebUIConnectionsUseCase = resolve()
fun KoinHelper.getObserveActiveSDWebUIConnectionUseCase(): ObserveActiveSDWebUIConnectionUseCase = resolve()
fun KoinHelper.getSaveSDWebUIConnectionUseCase(): SaveSDWebUIConnectionUseCase = resolve()
fun KoinHelper.getDeleteSDWebUIConnectionUseCase(): DeleteSDWebUIConnectionUseCase = resolve()
fun KoinHelper.getActivateSDWebUIConnectionUseCase(): ActivateSDWebUIConnectionUseCase = resolve()
fun KoinHelper.getTestSDWebUIConnectionUseCase(): TestSDWebUIConnectionUseCase = resolve()
fun KoinHelper.getFetchSDWebUIModelsUseCase(): FetchSDWebUIModelsUseCase = resolve()
fun KoinHelper.getFetchSDWebUISamplersUseCase(): FetchSDWebUISamplersUseCase = resolve()
fun KoinHelper.getFetchSDWebUIVaesUseCase(): FetchSDWebUIVaesUseCase = resolve()
fun KoinHelper.getGenerateSDWebUIImageUseCase(): GenerateSDWebUIImageUseCase = resolve()
fun KoinHelper.getInterruptSDWebUIGenerationUseCase(): InterruptSDWebUIGenerationUseCase = resolve()

// External Server
fun KoinHelper.getObserveExternalServerConfigsUseCase(): ObserveExternalServerConfigsUseCase = resolve()
fun KoinHelper.getObserveActiveExternalServerConfigUseCase(): ObserveActiveExternalServerConfigUseCase = resolve()
fun KoinHelper.getSaveExternalServerConfigUseCase(): SaveExternalServerConfigUseCase = resolve()
fun KoinHelper.getDeleteExternalServerConfigUseCase(): DeleteExternalServerConfigUseCase = resolve()
fun KoinHelper.getActivateExternalServerConfigUseCase(): ActivateExternalServerConfigUseCase = resolve()
fun KoinHelper.getTestExternalServerConnectionUseCase(): TestExternalServerConnectionUseCase = resolve()
fun KoinHelper.getGetExternalServerCapabilitiesUseCase(): GetExternalServerCapabilitiesUseCase = resolve()
fun KoinHelper.getGetExternalServerImagesUseCase(): GetExternalServerImagesUseCase = resolve()
fun KoinHelper.getGetGenerationOptionsUseCase(): GetGenerationOptionsUseCase = resolve()
fun KoinHelper.getGetDependentChoicesUseCase(): GetDependentChoicesUseCase = resolve()
fun KoinHelper.getExecuteGenerationUseCase(): ExecuteGenerationUseCase = resolve()
fun KoinHelper.getGetGenerationStatusUseCase(): GetGenerationStatusUseCase = resolve()
fun KoinHelper.getDeleteServerImagesUseCase(): DeleteServerImagesUseCase = resolve()
