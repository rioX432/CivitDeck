package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.AddImageToDatasetUseCase
import com.riox432.civitdeck.domain.usecase.CreateDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeleteDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetImagesUseCase
import com.riox432.civitdeck.domain.usecase.RemoveImageFromDatasetUseCase
import com.riox432.civitdeck.domain.usecase.RenameDatasetCollectionUseCase
import org.koin.dsl.module

val datasetDomainModule = module {
    factory { ObserveDatasetCollectionsUseCase(get()) }
    factory { CreateDatasetCollectionUseCase(get()) }
    factory { RenameDatasetCollectionUseCase(get()) }
    factory { DeleteDatasetCollectionUseCase(get()) }
    factory { ObserveDatasetImagesUseCase(get()) }
    factory { AddImageToDatasetUseCase(get()) }
    factory { RemoveImageFromDatasetUseCase(get()) }
}
