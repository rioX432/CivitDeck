package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.ObserveSeenTutorialVersionUseCase
import com.riox432.civitdeck.domain.usecase.SetSeenTutorialVersionUseCase
import org.koin.dsl.module

val tutorialDomainModule = module {
    factory { ObserveSeenTutorialVersionUseCase(get()) }
    factory { SetSeenTutorialVersionUseCase(get()) }
}
