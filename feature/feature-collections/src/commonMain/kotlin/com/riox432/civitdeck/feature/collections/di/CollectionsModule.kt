package com.riox432.civitdeck.feature.collections.di

import com.riox432.civitdeck.domain.repository.CollectionRepository
import com.riox432.civitdeck.feature.collections.data.repository.CollectionRepositoryImpl
import com.riox432.civitdeck.feature.collections.domain.usecase.BulkMoveModelsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.BulkRemoveModelsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.DeleteCollectionUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.ObserveCollectionModelsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.RenameCollectionUseCase
import com.riox432.civitdeck.feature.collections.presentation.CollectionDetailViewModel
import com.riox432.civitdeck.feature.collections.presentation.CollectionsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val collectionsModule = module {
    single<CollectionRepository> { CollectionRepositoryImpl(get()) }
    factory { RenameCollectionUseCase(get()) }
    factory { DeleteCollectionUseCase(get()) }
    factory { ObserveCollectionModelsUseCase(get()) }
    factory { BulkMoveModelsUseCase(get()) }
    factory { BulkRemoveModelsUseCase(get()) }
    viewModel { CollectionsViewModel(get(), get(), get(), get()) }
    viewModel { params -> CollectionDetailViewModel(params.get(), get(), get(), get(), get()) }
}
