package com.riox432.civitdeck.feature.collections.di

import com.riox432.civitdeck.domain.repository.CollectionRepository
import com.riox432.civitdeck.feature.collections.data.repository.CollectionRepositoryImpl
import com.riox432.civitdeck.feature.collections.domain.usecase.AddModelToCollectionUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.BulkMoveModelsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.BulkRemoveModelsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.CreateCollectionUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.DeleteCollectionUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.ObserveCollectionModelsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.ObserveCollectionsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.ObserveModelCollectionsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.RemoveModelFromCollectionUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.RenameCollectionUseCase
import org.koin.dsl.module

val collectionsModule = module {
    single<CollectionRepository> { CollectionRepositoryImpl(get()) }
    factory { ObserveCollectionsUseCase(get()) }
    factory { CreateCollectionUseCase(get()) }
    factory { RenameCollectionUseCase(get()) }
    factory { DeleteCollectionUseCase(get()) }
    factory { ObserveCollectionModelsUseCase(get()) }
    factory { AddModelToCollectionUseCase(get()) }
    factory { RemoveModelFromCollectionUseCase(get()) }
    factory { ObserveModelCollectionsUseCase(get()) }
    factory { BulkMoveModelsUseCase(get()) }
    factory { BulkRemoveModelsUseCase(get()) }
}
