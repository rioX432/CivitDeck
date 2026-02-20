package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.api.ApiKeyProvider
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatform

val sharedModules: List<Module>
    get() = listOf(
        platformModule,
        dataModule,
        domainModule,
    )

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(sharedModules)
    }
}

suspend fun initializeAuth() {
    val repository: UserPreferencesRepository = KoinPlatform.getKoin().get()
    val provider: ApiKeyProvider = KoinPlatform.getKoin().get()
    provider.apiKey = repository.getApiKey()
}
