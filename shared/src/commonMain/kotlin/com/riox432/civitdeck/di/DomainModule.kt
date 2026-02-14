package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.AddExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.AddSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ClearBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ClearCacheUseCase
import com.riox432.civitdeck.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.DeleteSavedPromptUseCase
import com.riox432.civitdeck.domain.usecase.GetCreatorModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetExcludedTagsUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetImagesUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetRecommendationsUseCase
import com.riox432.civitdeck.domain.usecase.GetViewedModelIdsUseCase
import com.riox432.civitdeck.domain.usecase.HideModelUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.ObserveFavoritesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObserveSavedPromptsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.RemoveExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.SavePromptUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.SetGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.SetNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
import com.riox432.civitdeck.domain.usecase.UnhideModelUseCase
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
    factory { GetRecommendationsUseCase(get(), get(), get(), get()) }
    factory { GetViewedModelIdsUseCase(get()) }
    factory { GetExcludedTagsUseCase(get()) }
    factory { AddExcludedTagUseCase(get()) }
    factory { RemoveExcludedTagUseCase(get()) }
    factory { GetHiddenModelIdsUseCase(get()) }
    factory { HideModelUseCase(get()) }
    factory { UnhideModelUseCase(get()) }
    factory { ObserveDefaultSortOrderUseCase(get()) }
    factory { SetDefaultSortOrderUseCase(get()) }
    factory { ObserveDefaultTimePeriodUseCase(get()) }
    factory { SetDefaultTimePeriodUseCase(get()) }
    factory { ObserveGridColumnsUseCase(get()) }
    factory { SetGridColumnsUseCase(get()) }
    factory { GetHiddenModelsUseCase(get()) }
    factory { ClearBrowsingHistoryUseCase(get()) }
    factory { ClearCacheUseCase(get()) }
}
