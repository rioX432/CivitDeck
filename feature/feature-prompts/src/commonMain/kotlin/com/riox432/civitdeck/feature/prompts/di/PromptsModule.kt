package com.riox432.civitdeck.feature.prompts.di

import com.riox432.civitdeck.data.local.CivitDeckDatabase
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import com.riox432.civitdeck.domain.repository.SavedReferenceRepository
import com.riox432.civitdeck.feature.prompts.data.repository.SavedPromptRepositoryImpl
import com.riox432.civitdeck.feature.prompts.domain.usecase.DeleteSavedPromptUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ObserveSavedPromptsUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ObserveTemplatesUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.SearchSavedPromptsUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ToggleTemplateUseCase
import com.riox432.civitdeck.feature.prompts.presentation.SavedPromptsViewModel
import com.riox432.civitdeck.feature.prompts.presentation.SavedReferenceViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module

val promptsModule = module {
    single { SavedPromptRepositoryImpl(get(), get<CivitDeckDatabase>().savedReferenceDao()) } withOptions {
        bind<SavedPromptRepository>()
        bind<SavedReferenceRepository>()
    }
    factory { ObserveSavedPromptsUseCase(get()) }
    factory { DeleteSavedPromptUseCase(get()) }
    factory { ToggleTemplateUseCase(get()) }
    factory { SearchSavedPromptsUseCase(get()) }
    factory { ObserveTemplatesUseCase(get()) }
    viewModel { SavedPromptsViewModel(get(), get(), get(), get(), get()) }
    viewModel { SavedReferenceViewModel(get(), get()) }
}
