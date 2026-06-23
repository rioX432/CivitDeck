package com.riox432.civitdeck.feature.comfyui.presentation

import com.riox432.civitdeck.data.image.SaveGeneratedImageUseCase
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
import com.riox432.civitdeck.domain.service.AppLifecycleTracker
import com.riox432.civitdeck.domain.service.BackgroundMonitorStarter
import com.riox432.civitdeck.domain.service.GenerationNotificationService
import com.riox432.civitdeck.domain.usecase.ObserveGenerationNotificationsEnabledUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ExtractWorkflowParametersUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUICheckpointsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUIControlNetsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUILorasUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchObjectInfoUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.InterruptComfyUIGenerationUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveGenerationProgressUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.PollComfyUIResultUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SubmitComfyUIGenerationUseCase

/**
 * Server resource loading: checkpoints, LoRAs, ControlNets, object info, and dynamic
 * workflow parameter extraction. Consumed by [GenerationResourceLoader].
 */
data class GenerationResourceUseCases(
    val fetchCheckpoints: FetchComfyUICheckpointsUseCase,
    val fetchLoras: FetchComfyUILorasUseCase,
    val fetchControlNets: FetchComfyUIControlNetsUseCase,
    val fetchObjectInfo: FetchObjectInfoUseCase,
    val extractParameters: ExtractWorkflowParametersUseCase,
)

/**
 * Generation execution: submitting, polling/progress, interruption, image saving, plus the
 * connection repository and notification/lifecycle services. Consumed by
 * [GenerationExecutionDelegate].
 */
data class GenerationExecutionUseCases(
    val submitGeneration: SubmitComfyUIGenerationUseCase,
    val pollResult: PollComfyUIResultUseCase,
    val observeProgress: ObserveGenerationProgressUseCase,
    val interruptGeneration: InterruptComfyUIGenerationUseCase,
    val saveImage: SaveGeneratedImageUseCase,
    val observeGenNotifEnabled: ObserveGenerationNotificationsEnabledUseCase,
    val repository: ComfyUIConnectionRepository,
    val notificationService: GenerationNotificationService,
    val lifecycleTracker: AppLifecycleTracker,
    val backgroundMonitorStarter: BackgroundMonitorStarter,
)
