package com.riox432.civitdeck.di

import com.riox432.civitdeck.ui.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    // ViewModels (shared between Android and iOS)
    viewModel {
        SettingsViewModel(
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(),
            get(), get(), get(), get(),
        )
    }
}
