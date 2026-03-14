package com.riox432.civitdeck.di

import com.riox432.civitdeck.feature.collections.presentation.CollectionDetailViewModel
import com.riox432.civitdeck.feature.collections.presentation.CollectionsViewModel
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import com.riox432.civitdeck.ui.analytics.DesktopAnalyticsViewModel
import com.riox432.civitdeck.ui.feed.DesktopFeedViewModel
import com.riox432.civitdeck.ui.search.DesktopSearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val desktopModule = module {
    viewModel {
        DesktopSearchViewModel(get())
    }
    viewModel { params ->
        ModelDetailViewModel(
            params.get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel {
        CollectionsViewModel(get(), get(), get(), get())
    }
    viewModel { params ->
        CollectionDetailViewModel(params.get(), get(), get(), get(), get())
    }
    viewModel {
        DesktopFeedViewModel(get(), get())
    }
    viewModel {
        DesktopAnalyticsViewModel(get())
    }
}
