package com.riox432.civitdeck.feature.search.di

import com.riox432.civitdeck.domain.repository.ExcludedTagRepository
import com.riox432.civitdeck.domain.repository.HiddenModelRepository
import com.riox432.civitdeck.domain.repository.SavedSearchFilterRepository
import com.riox432.civitdeck.domain.repository.SearchHistoryRepository
import com.riox432.civitdeck.feature.search.data.repository.ExcludedTagRepositoryImpl
import com.riox432.civitdeck.feature.search.data.repository.HiddenModelRepositoryImpl
import com.riox432.civitdeck.feature.search.data.repository.SavedSearchFilterRepositoryImpl
import com.riox432.civitdeck.feature.search.data.repository.SearchHistoryRepositoryImpl
import com.riox432.civitdeck.feature.search.domain.usecase.AddSearchHistoryUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.DeleteSavedSearchFilterUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.DeleteSearchHistoryItemUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetDiscoveryModelsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetHiddenModelIdsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetRecommendationsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.HideModelUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.MultiSourceSearchUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.ObserveSavedSearchFiltersUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.ObserveSearchHistoryUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.SaveSearchFilterUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.TrackRecommendationClickUseCase
import com.riox432.civitdeck.feature.search.presentation.BrowsingHistoryViewModel
import com.riox432.civitdeck.feature.search.presentation.ModelSearchViewModel
import com.riox432.civitdeck.feature.search.presentation.SwipeDiscoveryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val searchModule = module {
    // Repositories
    single<SearchHistoryRepository> { SearchHistoryRepositoryImpl(get()) }
    single<ExcludedTagRepository> { ExcludedTagRepositoryImpl(get()) }
    single<HiddenModelRepository> { HiddenModelRepositoryImpl(get()) }
    single<SavedSearchFilterRepository> { SavedSearchFilterRepositoryImpl(get()) }

    // Use cases
    factory { GetModelsUseCase(get()) }
    factory { MultiSourceSearchUseCase(get(), get(), get()) }
    factory { GetDiscoveryModelsUseCase(get()) }
    factory { GetRecommendationsUseCase(get(), get(), get(), get(), get()) }
    factory { ObserveSearchHistoryUseCase(get()) }
    factory { AddSearchHistoryUseCase(get()) }
    factory { DeleteSearchHistoryItemUseCase(get()) }
    factory { GetHiddenModelIdsUseCase(get()) }
    factory { HideModelUseCase(get()) }
    factory { ObserveSavedSearchFiltersUseCase(get()) }
    factory { SaveSearchFilterUseCase(get()) }
    factory { DeleteSavedSearchFilterUseCase(get()) }
    factory { TrackRecommendationClickUseCase(get()) }

    // ViewModels
    viewModel {
        ModelSearchViewModel(
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(),
            get(),
        )
    }
    viewModel { SwipeDiscoveryViewModel(get(), get()) }
    viewModel { BrowsingHistoryViewModel(get(), get(), get()) }
}
