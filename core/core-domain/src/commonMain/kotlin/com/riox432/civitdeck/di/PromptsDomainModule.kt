package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.AutoSavePromptUseCase
import com.riox432.civitdeck.domain.usecase.SavePromptUseCase
import org.koin.dsl.module

val promptsDomainModule = module {
    factory { SavePromptUseCase(get()) }
    factory { AutoSavePromptUseCase(get()) }
}
