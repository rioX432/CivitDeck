package com.riox432.civitdeck.feature.settings.di

import com.riox432.civitdeck.domain.repository.AppBehaviorPreferencesRepository
import com.riox432.civitdeck.domain.repository.AuthPreferencesRepository
import com.riox432.civitdeck.domain.repository.ContentFilterPreferencesRepository
import com.riox432.civitdeck.domain.repository.DisplayPreferencesRepository
import com.riox432.civitdeck.domain.repository.StoragePreferencesRepository
import com.riox432.civitdeck.feature.settings.data.repository.UserPreferencesRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val settingsModule = module {
    singleOf(::UserPreferencesRepositoryImpl) {
        bind<ContentFilterPreferencesRepository>()
        bind<DisplayPreferencesRepository>()
        bind<AuthPreferencesRepository>()
        bind<AppBehaviorPreferencesRepository>()
        bind<StoragePreferencesRepository>()
    }
}
