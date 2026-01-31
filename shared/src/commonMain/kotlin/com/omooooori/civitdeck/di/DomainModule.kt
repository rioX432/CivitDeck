package com.omooooori.civitdeck.di

import com.omooooori.civitdeck.domain.usecase.GetModelsUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { GetModelsUseCase(get()) }
}
