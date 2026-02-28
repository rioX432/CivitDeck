package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.ClearBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
import org.koin.dsl.module

val historyDomainModule = module {
    factory { TrackModelViewUseCase(get()) }
    factory { GetViewedModelIdsUseCase(get()) }
    factory { GetHiddenModelsUseCase(get()) }
    factory { ClearBrowsingHistoryUseCase(get()) }
}
