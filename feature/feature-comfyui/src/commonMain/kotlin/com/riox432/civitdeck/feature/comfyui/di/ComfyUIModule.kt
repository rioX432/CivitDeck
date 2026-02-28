package com.riox432.civitdeck.feature.comfyui.di

import com.riox432.civitdeck.domain.repository.ComfyUIRepository
import com.riox432.civitdeck.feature.comfyui.data.repository.ComfyUIRepositoryImpl
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ActivateComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.DeleteComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUICheckpointsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveActiveComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveComfyUIConnectionsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveGenerationProgressUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.PollComfyUIResultUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SaveComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SubmitComfyUIGenerationUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.TestComfyUIConnectionUseCase
import org.koin.dsl.module

val comfyuiModule = module {
    single<ComfyUIRepository> { ComfyUIRepositoryImpl(get(), get(), get()) }
    factory { ObserveComfyUIConnectionsUseCase(get()) }
    factory { ObserveActiveComfyUIConnectionUseCase(get()) }
    factory { SaveComfyUIConnectionUseCase(get()) }
    factory { DeleteComfyUIConnectionUseCase(get()) }
    factory { ActivateComfyUIConnectionUseCase(get()) }
    factory { TestComfyUIConnectionUseCase(get()) }
    factory { FetchComfyUICheckpointsUseCase(get()) }
    factory { SubmitComfyUIGenerationUseCase(get()) }
    factory { PollComfyUIResultUseCase(get()) }
    factory { ObserveGenerationProgressUseCase(get()) }
}
