package com.riox432.civitdeck.feature.detail.di

import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val detailModule = module {
    viewModel { params ->
        ModelDetailViewModel(
            params.get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
        )
    }
}
