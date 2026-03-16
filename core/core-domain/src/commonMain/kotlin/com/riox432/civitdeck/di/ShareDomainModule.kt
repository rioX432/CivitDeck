package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.AddShareHashtagUseCase
import com.riox432.civitdeck.domain.usecase.ObserveShareHashtagsUseCase
import com.riox432.civitdeck.domain.usecase.RemoveShareHashtagUseCase
import com.riox432.civitdeck.domain.usecase.ToggleShareHashtagUseCase
import org.koin.dsl.module

val shareDomainModule = module {
    factory { ObserveShareHashtagsUseCase(get()) }
    factory { AddShareHashtagUseCase(get()) }
    factory { RemoveShareHashtagUseCase(get()) }
    factory { ToggleShareHashtagUseCase(get()) }
}
