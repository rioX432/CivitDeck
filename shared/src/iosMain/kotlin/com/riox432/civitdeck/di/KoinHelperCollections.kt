@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.AddImageToDatasetUseCase
import com.riox432.civitdeck.domain.usecase.AddModelDirectoryUseCase
import com.riox432.civitdeck.domain.usecase.AddModelToCollectionUseCase
import com.riox432.civitdeck.domain.usecase.BatchEditTagsUseCase
import com.riox432.civitdeck.domain.usecase.CreateCollectionUseCase
import com.riox432.civitdeck.domain.usecase.CreateDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeleteDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DetectDuplicatesUseCase
import com.riox432.civitdeck.domain.usecase.EditCaptionUseCase
import com.riox432.civitdeck.domain.usecase.ExportDatasetUseCase
import com.riox432.civitdeck.domain.usecase.FilterByResolutionUseCase
import com.riox432.civitdeck.domain.usecase.GetNonTrainableImagesUseCase
import com.riox432.civitdeck.domain.usecase.GetTagSuggestionsUseCase
import com.riox432.civitdeck.domain.usecase.MarkImageExcludedUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetImagesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveFavoritesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveLocalModelFilesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelDirectoriesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveOwnedModelHashesUseCase
import com.riox432.civitdeck.domain.usecase.RemoveImageFromDatasetUseCase
import com.riox432.civitdeck.domain.usecase.RemoveModelDirectoryUseCase
import com.riox432.civitdeck.domain.usecase.RemoveModelFromCollectionUseCase
import com.riox432.civitdeck.domain.usecase.RenameDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.ScanModelDirectoriesUseCase
import com.riox432.civitdeck.domain.usecase.StoreImageDimensionsUseCase
import com.riox432.civitdeck.domain.usecase.StorePHashUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.UpdateTrainableUseCase
import com.riox432.civitdeck.domain.usecase.VerifyModelHashUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.BulkMoveModelsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.BulkRemoveModelsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.DeleteCollectionUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.ExportWithPluginUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.GetAvailableExportFormatsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.ObserveCollectionModelsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.RenameCollectionUseCase
import org.koin.mp.KoinPlatform.getKoin

// Favorites
fun KoinHelper.getToggleFavoriteUseCase(): ToggleFavoriteUseCase = getKoin().get()
fun KoinHelper.getObserveFavoritesUseCase(): ObserveFavoritesUseCase = getKoin().get()
fun KoinHelper.getObserveIsFavoriteUseCase(): ObserveIsFavoriteUseCase = getKoin().get()

// Collections
fun KoinHelper.getObserveCollectionsUseCase(): ObserveCollectionsUseCase = getKoin().get()
fun KoinHelper.getCreateCollectionUseCase(): CreateCollectionUseCase = getKoin().get()
fun KoinHelper.getRenameCollectionUseCase(): RenameCollectionUseCase = getKoin().get()
fun KoinHelper.getDeleteCollectionUseCase(): DeleteCollectionUseCase = getKoin().get()
fun KoinHelper.getObserveCollectionModelsUseCase(): ObserveCollectionModelsUseCase = getKoin().get()
fun KoinHelper.getAddModelToCollectionUseCase(): AddModelToCollectionUseCase = getKoin().get()
fun KoinHelper.getRemoveModelFromCollectionUseCase(): RemoveModelFromCollectionUseCase = getKoin().get()
fun KoinHelper.getObserveModelCollectionsUseCase(): ObserveModelCollectionsUseCase = getKoin().get()
fun KoinHelper.getBulkMoveModelsUseCase(): BulkMoveModelsUseCase = getKoin().get()
fun KoinHelper.getBulkRemoveModelsUseCase(): BulkRemoveModelsUseCase = getKoin().get()

// Local Model Files
fun KoinHelper.getObserveModelDirectoriesUseCase(): ObserveModelDirectoriesUseCase = getKoin().get()
fun KoinHelper.getAddModelDirectoryUseCase(): AddModelDirectoryUseCase = getKoin().get()
fun KoinHelper.getRemoveModelDirectoryUseCase(): RemoveModelDirectoryUseCase = getKoin().get()
fun KoinHelper.getObserveLocalModelFilesUseCase(): ObserveLocalModelFilesUseCase = getKoin().get()
fun KoinHelper.getScanModelDirectoriesUseCase(): ScanModelDirectoriesUseCase = getKoin().get()
fun KoinHelper.getVerifyModelHashUseCase(): VerifyModelHashUseCase = getKoin().get()
fun KoinHelper.getObserveOwnedModelHashesUseCase(): ObserveOwnedModelHashesUseCase = getKoin().get()

// Dataset
fun KoinHelper.getObserveDatasetCollectionsUseCase(): ObserveDatasetCollectionsUseCase = getKoin().get()
fun KoinHelper.getCreateDatasetCollectionUseCase(): CreateDatasetCollectionUseCase = getKoin().get()
fun KoinHelper.getRenameDatasetCollectionUseCase(): RenameDatasetCollectionUseCase = getKoin().get()
fun KoinHelper.getDeleteDatasetCollectionUseCase(): DeleteDatasetCollectionUseCase = getKoin().get()
fun KoinHelper.getObserveDatasetImagesUseCase(): ObserveDatasetImagesUseCase = getKoin().get()
fun KoinHelper.getAddImageToDatasetUseCase(): AddImageToDatasetUseCase = getKoin().get()
fun KoinHelper.getRemoveImageFromDatasetUseCase(): RemoveImageFromDatasetUseCase = getKoin().get()
fun KoinHelper.getBatchEditTagsUseCase(): BatchEditTagsUseCase = getKoin().get()
fun KoinHelper.getEditCaptionUseCase(): EditCaptionUseCase = getKoin().get()
fun KoinHelper.getGetTagSuggestionsUseCase(): GetTagSuggestionsUseCase = getKoin().get()
fun KoinHelper.getUpdateTrainableUseCase(): UpdateTrainableUseCase = getKoin().get()
fun KoinHelper.getGetNonTrainableImagesUseCase(): GetNonTrainableImagesUseCase = getKoin().get()
fun KoinHelper.getDetectDuplicatesUseCase(): DetectDuplicatesUseCase = getKoin().get()
fun KoinHelper.getFilterByResolutionUseCase(): FilterByResolutionUseCase = getKoin().get()
fun KoinHelper.getMarkImageExcludedUseCase(): MarkImageExcludedUseCase = getKoin().get()
fun KoinHelper.getStorePHashUseCase(): StorePHashUseCase = getKoin().get()
fun KoinHelper.getStoreImageDimensionsUseCase(): StoreImageDimensionsUseCase = getKoin().get()
fun KoinHelper.getExportDatasetUseCase(): ExportDatasetUseCase = getKoin().get()
fun KoinHelper.getGetAvailableExportFormatsUseCase(): GetAvailableExportFormatsUseCase = getKoin().get()
fun KoinHelper.getExportWithPluginUseCase(): ExportWithPluginUseCase = getKoin().get()
