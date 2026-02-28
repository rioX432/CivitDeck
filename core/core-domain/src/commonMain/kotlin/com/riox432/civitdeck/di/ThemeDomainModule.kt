package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.ObserveAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.SetAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.SetAmoledDarkModeUseCase
import org.koin.dsl.module

val themeDomainModule = module {
    factory { ObserveAccentColorUseCase(get()) }
    factory { SetAccentColorUseCase(get()) }
    factory { ObserveAmoledDarkModeUseCase(get()) }
    factory { SetAmoledDarkModeUseCase(get()) }
}
