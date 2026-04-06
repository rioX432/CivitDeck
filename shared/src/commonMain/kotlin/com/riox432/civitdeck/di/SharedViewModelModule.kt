package com.riox432.civitdeck.di

import com.riox432.civitdeck.presentation.comfyhub.ComfyHubBrowserViewModel
import com.riox432.civitdeck.presentation.comfyhub.ComfyHubDetailViewModel
import com.riox432.civitdeck.presentation.modelfiles.ModelFileBrowserViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sharedViewModelModule = module {
    viewModel { ComfyHubBrowserViewModel(get()) }
    viewModel { params -> ComfyHubDetailViewModel(params.get(), get(), get()) }
    viewModel { ModelFileBrowserViewModel(get(), get(), get(), get(), get(), get()) }
}
