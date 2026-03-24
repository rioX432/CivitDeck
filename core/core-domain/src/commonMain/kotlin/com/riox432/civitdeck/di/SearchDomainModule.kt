package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.AddExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.GetExcludedTagsUseCase
import com.riox432.civitdeck.domain.usecase.RemoveExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.UnhideModelUseCase
import org.koin.dsl.module

val searchDomainModule = module {
    factory { ClearSearchHistoryUseCase(get()) }
    factory { UnhideModelUseCase(get()) }
    factory { GetExcludedTagsUseCase(get()) }
    factory { AddExcludedTagUseCase(get()) }
    factory { RemoveExcludedTagUseCase(get()) }
}
