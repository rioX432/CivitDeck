package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.GetBrowsingStatsUseCase
import org.koin.dsl.module

val analyticsDomainModule = module {
    factory { GetBrowsingStatsUseCase(get()) }
}
