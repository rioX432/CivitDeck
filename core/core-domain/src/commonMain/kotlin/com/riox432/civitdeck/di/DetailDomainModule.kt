package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import org.koin.dsl.module

val detailDomainModule = module {
    factory { GetModelDetailUseCase(get()) }
}
