package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.AddPersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.DeleteModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.GetAllPersonalTagsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.ObservePersonalTagsUseCase
import com.riox432.civitdeck.domain.usecase.RemovePersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.SaveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.SearchModelsByTagUseCase
import org.koin.dsl.module

val notesDomainModule = module {
    factory { ObserveModelNoteUseCase(get()) }
    factory { SaveModelNoteUseCase(get()) }
    factory { DeleteModelNoteUseCase(get()) }
    factory { ObservePersonalTagsUseCase(get()) }
    factory { AddPersonalTagUseCase(get()) }
    factory { RemovePersonalTagUseCase(get()) }
    factory { GetAllPersonalTagsUseCase(get()) }
    factory { SearchModelsByTagUseCase(get()) }
}
