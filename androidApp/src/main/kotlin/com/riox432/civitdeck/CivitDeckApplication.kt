package com.riox432.civitdeck

import android.app.Application
import com.riox432.civitdeck.di.initKoin
import com.riox432.civitdeck.ui.creator.CreatorProfileViewModel
import com.riox432.civitdeck.ui.detail.ModelDetailViewModel
import com.riox432.civitdeck.ui.favorites.FavoritesViewModel
import com.riox432.civitdeck.ui.gallery.ImageGalleryViewModel
import com.riox432.civitdeck.ui.prompts.SavedPromptsViewModel
import com.riox432.civitdeck.ui.search.ModelSearchViewModel
import com.riox432.civitdeck.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class CivitDeckApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@CivitDeckApplication)
            modules(androidModule)
        }
    }
}

val androidModule = module {
    viewModel {
        ModelSearchViewModel(
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel { FavoritesViewModel(get()) }
    viewModel { params -> ModelDetailViewModel(params.get(), get(), get(), get(), get()) }
    viewModel { params -> CreatorProfileViewModel(params.get(), get()) }
    viewModel { params -> ImageGalleryViewModel(params.get(), get(), get()) }
    viewModel { SavedPromptsViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
}
