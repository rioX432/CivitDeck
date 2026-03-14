package com.riox432.civitdeck.di

import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
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
}
