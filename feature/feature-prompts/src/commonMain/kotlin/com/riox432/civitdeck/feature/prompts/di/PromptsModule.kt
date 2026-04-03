package com.riox432.civitdeck.feature.prompts.di

import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import com.riox432.civitdeck.feature.prompts.data.repository.SavedPromptRepositoryImpl
import com.riox432.civitdeck.feature.prompts.domain.usecase.DeleteSavedPromptUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ObserveSavedPromptsUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ObserveTemplatesUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.SearchSavedPromptsUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ToggleTemplateUseCase
import org.koin.dsl.module

val promptsModule = module {
    single<SavedPromptRepository> { SavedPromptRepositoryImpl(get()) }
    factory { ObserveSavedPromptsUseCase(get()) }
    factory { DeleteSavedPromptUseCase(get()) }
    factory { ToggleTemplateUseCase(get()) }
    factory { SearchSavedPromptsUseCase(get()) }
    factory { ObserveTemplatesUseCase(get()) }
}
