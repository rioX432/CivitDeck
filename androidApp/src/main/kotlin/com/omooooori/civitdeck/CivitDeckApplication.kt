package com.omooooori.civitdeck

import android.app.Application
import com.omooooori.civitdeck.di.initKoin
import com.omooooori.civitdeck.ui.detail.ModelDetailViewModel
import com.omooooori.civitdeck.ui.gallery.ImageGalleryViewModel
import com.omooooori.civitdeck.ui.search.ModelSearchViewModel
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
    viewModel { ModelSearchViewModel(get()) }
    viewModel { params -> ModelDetailViewModel(params.get(), get(), get(), get()) }
    viewModel { params -> ImageGalleryViewModel(params.get(), get()) }
}
