package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.ObserveApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.SetApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.ValidateApiKeyUseCase
import org.koin.dsl.module

val authDomainModule = module {
    factory { ObserveApiKeyUseCase(get()) }
    factory { SetApiKeyUseCase(get()) }
    factory { ValidateApiKeyUseCase(get()) }
}
