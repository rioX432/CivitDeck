package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.api.ApiKeyProvider
import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.data.api.comfyui.createComfyUIHttpClient
import com.riox432.civitdeck.data.api.createHttpClient
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

val networkModule = module {
    single { ApiKeyProvider() }
    single { createHttpClient(get()) }
    single { CivitAiApi(get()) }
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    // ComfyUI
    single(named("comfyui")) { createComfyUIHttpClient() }
    single { ComfyUIApi(get(named("comfyui")), get()) }
}
