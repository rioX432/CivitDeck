package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.CheckForUpdateUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAutoUpdateCheckUseCase
import com.riox432.civitdeck.domain.usecase.SetAutoUpdateCheckUseCase
import org.koin.dsl.module

val updateDomainModule = module {
    factory { CheckForUpdateUseCase(get()) }
    factory { ObserveAutoUpdateCheckUseCase(get()) }
    factory { SetAutoUpdateCheckUseCase(get()) }
}
