package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.GetSimilarModelsUseCase
import org.koin.dsl.module

val similarityDomainModule = module {
    factory { GetSimilarModelsUseCase(get()) }
}
