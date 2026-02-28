package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.ObserveCivitaiLinkKeyUseCase
import com.riox432.civitdeck.domain.usecase.SetCivitaiLinkKeyUseCase
import org.koin.dsl.module

val civitaiLinkDomainModule = module {
    factory { ObserveCivitaiLinkKeyUseCase(get()) }
    factory { SetCivitaiLinkKeyUseCase(get()) }
}
