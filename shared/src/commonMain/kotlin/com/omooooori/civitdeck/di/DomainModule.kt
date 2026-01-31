package com.omooooori.civitdeck.di

import com.omooooori.civitdeck.domain.usecase.GetModelDetailUseCase
import com.omooooori.civitdeck.domain.usecase.GetModelsUseCase
import com.omooooori.civitdeck.domain.usecase.ObserveFavoritesUseCase
import com.omooooori.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.omooooori.civitdeck.domain.usecase.ToggleFavoriteUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { GetModelsUseCase(get()) }
    factory { GetModelDetailUseCase(get()) }
    factory { ToggleFavoriteUseCase(get()) }
    factory { ObserveFavoritesUseCase(get()) }
    factory { ObserveIsFavoriteUseCase(get()) }
}
