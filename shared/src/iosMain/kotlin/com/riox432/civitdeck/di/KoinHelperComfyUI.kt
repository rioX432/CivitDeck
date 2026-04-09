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
import org.koin.mp.KoinPlatform.getKoin

// ComfyUI
fun KoinHelper.getObserveComfyUIConnectionsUseCase(): ObserveComfyUIConnectionsUseCase = getKoin().get()
fun KoinHelper.getObserveActiveComfyUIConnectionUseCase(): ObserveActiveComfyUIConnectionUseCase = getKoin().get()
fun KoinHelper.getSaveComfyUIConnectionUseCase(): SaveComfyUIConnectionUseCase = getKoin().get()
fun KoinHelper.getDeleteComfyUIConnectionUseCase(): DeleteComfyUIConnectionUseCase = getKoin().get()
fun KoinHelper.getActivateComfyUIConnectionUseCase(): ActivateComfyUIConnectionUseCase = getKoin().get()
fun KoinHelper.getTestComfyUIConnectionUseCase(): TestComfyUIConnectionUseCase = getKoin().get()
fun KoinHelper.getFetchComfyUICheckpointsUseCase(): FetchComfyUICheckpointsUseCase = getKoin().get()
fun KoinHelper.getFetchComfyUILorasUseCase(): FetchComfyUILorasUseCase = getKoin().get()
fun KoinHelper.getFetchComfyUIControlNetsUseCase(): FetchComfyUIControlNetsUseCase = getKoin().get()
fun KoinHelper.getImportWorkflowUseCase(): ImportWorkflowUseCase = getKoin().get()
fun KoinHelper.getSubmitComfyUIGenerationUseCase(): SubmitComfyUIGenerationUseCase = getKoin().get()
fun KoinHelper.getPollComfyUIResultUseCase(): PollComfyUIResultUseCase = getKoin().get()
fun KoinHelper.getObserveGenerationProgressUseCase(): ObserveGenerationProgressUseCase = getKoin().get()
fun KoinHelper.getObserveComfyUIQueueUseCase(): ObserveComfyUIQueueUseCase = getKoin().get()
fun KoinHelper.getCancelComfyUIJobUseCase(): CancelComfyUIJobUseCase = getKoin().get()
fun KoinHelper.getInterruptComfyUIGenerationUseCase(): InterruptComfyUIGenerationUseCase = getKoin().get()
fun KoinHelper.getComfyUIConnectionRepository(): ComfyUIConnectionRepository = getKoin().get()
fun KoinHelper.getFindMatchingLocalModelUseCase(): FindMatchingLocalModelUseCase = getKoin().get()
fun KoinHelper.getPopulateGenerationFromModelUseCase(): PopulateGenerationFromModelUseCase = getKoin().get()
fun KoinHelper.getSaveGeneratedImageUseCase(): SaveGeneratedImageUseCase = getKoin().get()
fun KoinHelper.getFetchComfyUIHistoryUseCase(): FetchComfyUIHistoryUseCase = getKoin().get()
fun KoinHelper.getFetchComfyUIHistoryItemUseCase(): FetchComfyUIHistoryItemUseCase = getKoin().get()

// ComfyUI Workflow Templates
fun KoinHelper.getGetWorkflowTemplatesUseCase(): GetWorkflowTemplatesUseCase = getKoin().get()
fun KoinHelper.getSaveWorkflowTemplateUseCase(): SaveWorkflowTemplateUseCase = getKoin().get()
fun KoinHelper.getDeleteWorkflowTemplateUseCase(): DeleteWorkflowTemplateUseCase = getKoin().get()
fun KoinHelper.getExportWorkflowTemplateUseCase(): ExportWorkflowTemplateUseCase = getKoin().get()
fun KoinHelper.getImportWorkflowTemplateUseCase(): ImportWorkflowTemplateUseCase = getKoin().get()
fun KoinHelper.getApplyWorkflowTemplateUseCase(): ApplyWorkflowTemplateUseCase = getKoin().get()

// ComfyHub
fun KoinHelper.getSearchComfyHubWorkflowsUseCase(): SearchComfyHubWorkflowsUseCase = getKoin().get()
fun KoinHelper.getGetComfyHubWorkflowDetailUseCase(): GetComfyHubWorkflowDetailUseCase = getKoin().get()
fun KoinHelper.getImportComfyHubWorkflowUseCase(): ImportComfyHubWorkflowUseCase = getKoin().get()

// Civitai Link
fun KoinHelper.getObserveCivitaiLinkKeyUseCase(): ObserveCivitaiLinkKeyUseCase = getKoin().get()
fun KoinHelper.getSetCivitaiLinkKeyUseCase(): SetCivitaiLinkKeyUseCase = getKoin().get()
fun KoinHelper.getObserveCivitaiLinkStatusUseCase(): ObserveCivitaiLinkStatusUseCase = getKoin().get()
fun KoinHelper.getObserveCivitaiLinkActivitiesUseCase(): ObserveCivitaiLinkActivitiesUseCase = getKoin().get()
fun KoinHelper.getConnectCivitaiLinkUseCase(): ConnectCivitaiLinkUseCase = getKoin().get()
fun KoinHelper.getDisconnectCivitaiLinkUseCase(): DisconnectCivitaiLinkUseCase = getKoin().get()
fun KoinHelper.getSendResourceToPCUseCase(): SendResourceToPCUseCase = getKoin().get()
fun KoinHelper.getCancelLinkActivityUseCase(): CancelLinkActivityUseCase = getKoin().get()

// SD WebUI
fun KoinHelper.getObserveSDWebUIConnectionsUseCase(): ObserveSDWebUIConnectionsUseCase = getKoin().get()
fun KoinHelper.getObserveActiveSDWebUIConnectionUseCase(): ObserveActiveSDWebUIConnectionUseCase = getKoin().get()
fun KoinHelper.getSaveSDWebUIConnectionUseCase(): SaveSDWebUIConnectionUseCase = getKoin().get()
fun KoinHelper.getDeleteSDWebUIConnectionUseCase(): DeleteSDWebUIConnectionUseCase = getKoin().get()
fun KoinHelper.getActivateSDWebUIConnectionUseCase(): ActivateSDWebUIConnectionUseCase = getKoin().get()
fun KoinHelper.getTestSDWebUIConnectionUseCase(): TestSDWebUIConnectionUseCase = getKoin().get()
fun KoinHelper.getFetchSDWebUIModelsUseCase(): FetchSDWebUIModelsUseCase = getKoin().get()
fun KoinHelper.getFetchSDWebUISamplersUseCase(): FetchSDWebUISamplersUseCase = getKoin().get()
fun KoinHelper.getFetchSDWebUIVaesUseCase(): FetchSDWebUIVaesUseCase = getKoin().get()
fun KoinHelper.getGenerateSDWebUIImageUseCase(): GenerateSDWebUIImageUseCase = getKoin().get()
fun KoinHelper.getInterruptSDWebUIGenerationUseCase(): InterruptSDWebUIGenerationUseCase = getKoin().get()

// External Server
fun KoinHelper.getObserveExternalServerConfigsUseCase(): ObserveExternalServerConfigsUseCase = getKoin().get()
fun KoinHelper.getObserveActiveExternalServerConfigUseCase(): ObserveActiveExternalServerConfigUseCase = getKoin().get()
fun KoinHelper.getSaveExternalServerConfigUseCase(): SaveExternalServerConfigUseCase = getKoin().get()
fun KoinHelper.getDeleteExternalServerConfigUseCase(): DeleteExternalServerConfigUseCase = getKoin().get()
fun KoinHelper.getActivateExternalServerConfigUseCase(): ActivateExternalServerConfigUseCase = getKoin().get()
fun KoinHelper.getTestExternalServerConnectionUseCase(): TestExternalServerConnectionUseCase = getKoin().get()
fun KoinHelper.getGetExternalServerCapabilitiesUseCase(): GetExternalServerCapabilitiesUseCase = getKoin().get()
fun KoinHelper.getGetExternalServerImagesUseCase(): GetExternalServerImagesUseCase = getKoin().get()
fun KoinHelper.getGetGenerationOptionsUseCase(): GetGenerationOptionsUseCase = getKoin().get()
fun KoinHelper.getGetDependentChoicesUseCase(): GetDependentChoicesUseCase = getKoin().get()
fun KoinHelper.getExecuteGenerationUseCase(): ExecuteGenerationUseCase = getKoin().get()
fun KoinHelper.getGetGenerationStatusUseCase(): GetGenerationStatusUseCase = getKoin().get()
fun KoinHelper.getDeleteServerImagesUseCase(): DeleteServerImagesUseCase = getKoin().get()
