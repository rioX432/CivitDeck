package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.AddSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.DeleteSavedPromptUseCase
import com.riox432.civitdeck.domain.usecase.GetCreatorModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetImagesUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetRecommendationsUseCase
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveFavoritesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObserveSavedPromptsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.SavePromptUseCase
import com.riox432.civitdeck.domain.usecase.SetNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { GetModelsUseCase(get()) }
    factory { GetCreatorModelsUseCase(get()) }
    factory { GetModelDetailUseCase(get()) }
    factory { GetImagesUseCase(get()) }
    factory { ToggleFavoriteUseCase(get()) }
    factory { ObserveFavoritesUseCase(get()) }
    factory { ObserveIsFavoriteUseCase(get()) }
    factory { ObserveNsfwFilterUseCase(get()) }
    factory { SetNsfwFilterUseCase(get()) }
    factory { SavePromptUseCase(get()) }
    factory { ObserveSavedPromptsUseCase(get()) }
    factory { DeleteSavedPromptUseCase(get()) }
    factory { ObserveSearchHistoryUseCase(get()) }
    factory { AddSearchHistoryUseCase(get()) }
    factory { ClearSearchHistoryUseCase(get()) }
    factory { TrackModelViewUseCase(get()) }
    factory { GetRecommendationsUseCase(get(), get(), get()) }
    factory { GetViewedModelIdsUseCase(get()) }
}
