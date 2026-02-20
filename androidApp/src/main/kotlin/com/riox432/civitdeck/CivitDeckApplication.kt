package com.riox432.civitdeck

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.riox432.civitdeck.di.initKoin
import com.riox432.civitdeck.di.initializeAuth
import com.riox432.civitdeck.ui.collections.CollectionDetailViewModel
import com.riox432.civitdeck.ui.collections.CollectionsViewModel
import com.riox432.civitdeck.ui.creator.CreatorProfileViewModel
import com.riox432.civitdeck.ui.detail.ModelDetailViewModel
import com.riox432.civitdeck.ui.gallery.ImageGalleryViewModel
import com.riox432.civitdeck.ui.prompts.SavedPromptsViewModel
import com.riox432.civitdeck.ui.search.ModelSearchViewModel
import com.riox432.civitdeck.ui.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class CivitDeckApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@CivitDeckApplication)
            modules(androidModule)
        }
        CoroutineScope(Dispatchers.IO).launch { initializeAuth() }
    }

    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}

val androidModule = module {
    viewModel {
        ModelSearchViewModel(
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(),
        )
    }
    viewModel { CollectionsViewModel(get(), get(), get(), get()) }
    viewModel { params -> CollectionDetailViewModel(params.get(), get(), get(), get(), get()) }
    viewModel { params ->
        ModelDetailViewModel(params.get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
    viewModel { params -> CreatorProfileViewModel(params.get(), get()) }
    viewModel { params -> ImageGalleryViewModel(params.get(), get(), get(), get()) }
    viewModel { SavedPromptsViewModel(get(), get()) }
    viewModel {
        SettingsViewModel(
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(),
        )
    }
}
