package com.riox432.civitdeck.feature.detail.di

import com.riox432.civitdeck.feature.detail.presentation.CollectionUseCases
import com.riox432.civitdeck.feature.detail.presentation.DownloadUseCases
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import com.riox432.civitdeck.feature.detail.presentation.ModelUseCases
import com.riox432.civitdeck.feature.detail.presentation.NotesTagsUseCases
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val detailModule = module {
    factory {
        ModelUseCases(
            getModelDetail = get(),
            observeIsFavorite = get(),
            toggleFavorite = get(),
            trackModelView = get(),
            enrichModelImages = get(),
            embedOnBrowse = get(),
            observeNsfwFilter = get(),
            observePowerUserMode = get(),
        )
    }
    factory {
        CollectionUseCases(
            observeCollections = get(),
            observeModelCollections = get(),
            addModelToCollection = get(),
            removeModelFromCollection = get(),
            createCollection = get(),
        )
    }
    factory {
        NotesTagsUseCases(
            observeModelNote = get(),
            saveModelNote = get(),
            deleteModelNote = get(),
            observePersonalTags = get(),
            addPersonalTag = get(),
            removePersonalTag = get(),
        )
    }
    factory { DownloadUseCases(observeModelDownloads = get(), enqueueDownload = get(), cancelDownload = get()) }
    viewModel { params ->
        ModelDetailViewModel(params.get(), get(), get(), get(), get(), get(), get())
    }
}
