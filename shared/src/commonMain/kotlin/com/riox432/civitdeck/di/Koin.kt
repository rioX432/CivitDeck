package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.api.ApiKeyProvider
import com.riox432.civitdeck.domain.repository.AuthPreferencesRepository
import com.riox432.civitdeck.feature.collections.di.collectionsModule
import com.riox432.civitdeck.feature.comfyui.di.comfyuiModule
import com.riox432.civitdeck.feature.creator.di.creatorModule
import com.riox432.civitdeck.feature.detail.di.detailModule
import com.riox432.civitdeck.feature.externalserver.di.externalServerModule
import com.riox432.civitdeck.feature.gallery.di.galleryModule
import com.riox432.civitdeck.feature.prompts.di.promptsModule
import com.riox432.civitdeck.feature.search.di.searchModule
import com.riox432.civitdeck.feature.settings.di.settingsModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatform

val sharedModules: List<Module>
    get() = listOf(
        platformModule,
        networkModule,
        databaseModule,
        dataModule,
        domainModule,
        settingsModule,
        creatorModule,
        promptsModule,
        galleryModule,
        collectionsModule,
        detailModule,
        comfyuiModule,
        searchModule,
        externalServerModule,
    )

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(sharedModules)
    }
}

suspend fun initializeAuth() {
    val repository: AuthPreferencesRepository = KoinPlatform.getKoin().get()
    val provider: ApiKeyProvider = KoinPlatform.getKoin().get()
    provider.apiKey = repository.getApiKey()
}
