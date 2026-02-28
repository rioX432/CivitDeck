package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.ObserveNsfwBlurSettingsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.SetNsfwBlurSettingsUseCase
import com.riox432.civitdeck.domain.usecase.SetNsfwFilterUseCase
import org.koin.dsl.module

val nsfwDomainModule = module {
    factory { ObserveNsfwFilterUseCase(get()) }
    factory { SetNsfwFilterUseCase(get()) }
    factory { ObserveNsfwBlurSettingsUseCase(get()) }
    factory { SetNsfwBlurSettingsUseCase(get()) }
}
