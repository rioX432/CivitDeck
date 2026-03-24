package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.AddModelToCollectionUseCase
import com.riox432.civitdeck.domain.usecase.CreateCollectionUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.RemoveModelFromCollectionUseCase
import org.koin.dsl.module

val collectionsDomainModule = module {
    factory { ObserveCollectionsUseCase(get()) }
    factory { CreateCollectionUseCase(get()) }
    factory { AddModelToCollectionUseCase(get()) }
    factory { RemoveModelFromCollectionUseCase(get()) }
    factory { ObserveModelCollectionsUseCase(get()) }
}
