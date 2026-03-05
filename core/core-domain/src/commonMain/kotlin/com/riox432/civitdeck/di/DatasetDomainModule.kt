package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.AddImageToDatasetUseCase
import com.riox432.civitdeck.domain.usecase.BatchEditTagsUseCase
import com.riox432.civitdeck.domain.usecase.CreateDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeleteDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DetectDuplicatesUseCase
import com.riox432.civitdeck.domain.usecase.ExportDatasetUseCase
import com.riox432.civitdeck.domain.usecase.EditCaptionUseCase
import com.riox432.civitdeck.domain.usecase.FilterByResolutionUseCase
import com.riox432.civitdeck.domain.usecase.GetModelLicenseUseCase
import com.riox432.civitdeck.domain.usecase.GetNonTrainableImagesUseCase
import com.riox432.civitdeck.domain.usecase.GetTagSuggestionsUseCase
import com.riox432.civitdeck.domain.usecase.MarkImageExcludedUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetImagesUseCase
import com.riox432.civitdeck.domain.usecase.RemoveImageFromDatasetUseCase
import com.riox432.civitdeck.domain.usecase.RenameDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.StoreImageDimensionsUseCase
import com.riox432.civitdeck.domain.usecase.StorePHashUseCase
import com.riox432.civitdeck.domain.usecase.UpdateTrainableUseCase
import org.koin.dsl.module

val datasetDomainModule = module {
    factory { ObserveDatasetCollectionsUseCase(get()) }
    factory { CreateDatasetCollectionUseCase(get()) }
    factory { RenameDatasetCollectionUseCase(get()) }
    factory { DeleteDatasetCollectionUseCase(get()) }
    factory { ObserveDatasetImagesUseCase(get()) }
    factory { AddImageToDatasetUseCase(get()) }
    factory { RemoveImageFromDatasetUseCase(get()) }
    factory { BatchEditTagsUseCase(get()) }
    factory { EditCaptionUseCase(get()) }
    factory { GetTagSuggestionsUseCase(get()) }
    factory { UpdateTrainableUseCase(get()) }
    factory { GetNonTrainableImagesUseCase(get()) }
    factory { GetModelLicenseUseCase(get()) }
    factory { DetectDuplicatesUseCase(get()) }
    factory { FilterByResolutionUseCase(get()) }
    factory { MarkImageExcludedUseCase(get()) }
    factory { StorePHashUseCase(get()) }
    factory { StoreImageDimensionsUseCase(get()) }
    factory { ExportDatasetUseCase(get()) }
}
