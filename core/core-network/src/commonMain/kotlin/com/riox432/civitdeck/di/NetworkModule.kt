package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.api.ApiKeyProvider
import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.data.api.GitHubReleaseApi
import com.riox432.civitdeck.data.api.civitailink.CivitaiLinkApi
import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.data.api.comfyui.ComfyUIWebSocketApi
import com.riox432.civitdeck.data.api.comfyui.createComfyUIHttpClient
import com.riox432.civitdeck.data.api.createHttpClient
import com.riox432.civitdeck.data.api.externalserver.ExternalServerApi
import com.riox432.civitdeck.data.api.externalserver.createExternalServerHttpClient
import com.riox432.civitdeck.data.api.webui.SDWebUIApi
import com.riox432.civitdeck.data.api.webui.createSDWebUIHttpClient
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

    // GitHub Releases
    single { GitHubReleaseApi(get()) }

    // ComfyUI
    single(named("comfyui")) { createComfyUIHttpClient() }
    single { ComfyUIApi(get(named("comfyui")), get()) }
    single { ComfyUIWebSocketApi(get(named("comfyui")), get()) }

    // SDWebUI
    single(named("sdwebui")) { createSDWebUIHttpClient() }
    single { SDWebUIApi(get(named("sdwebui"))) }

    // Civitai Link uses the ComfyUI client (already has WebSockets plugin)
    single { CivitaiLinkApi(get(named("comfyui")), get()) }

    // External Server (generic REST API)
    single(named("externalserver")) { createExternalServerHttpClient() }
    single { ExternalServerApi(get(named("externalserver"))) }
}
