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

// Favorites
fun KoinHelper.getToggleFavoriteUseCase(): ToggleFavoriteUseCase = resolve()
fun KoinHelper.getObserveFavoritesUseCase(): ObserveFavoritesUseCase = resolve()
fun KoinHelper.getObserveIsFavoriteUseCase(): ObserveIsFavoriteUseCase = resolve()

// Collections
fun KoinHelper.getObserveCollectionsUseCase(): ObserveCollectionsUseCase = resolve()
fun KoinHelper.getCreateCollectionUseCase(): CreateCollectionUseCase = resolve()
fun KoinHelper.getRenameCollectionUseCase(): RenameCollectionUseCase = resolve()
fun KoinHelper.getDeleteCollectionUseCase(): DeleteCollectionUseCase = resolve()
fun KoinHelper.getObserveCollectionModelsUseCase(): ObserveCollectionModelsUseCase = resolve()
fun KoinHelper.getAddModelToCollectionUseCase(): AddModelToCollectionUseCase = resolve()
fun KoinHelper.getRemoveModelFromCollectionUseCase(): RemoveModelFromCollectionUseCase = resolve()
fun KoinHelper.getObserveModelCollectionsUseCase(): ObserveModelCollectionsUseCase = resolve()
fun KoinHelper.getBulkMoveModelsUseCase(): BulkMoveModelsUseCase = resolve()
fun KoinHelper.getBulkRemoveModelsUseCase(): BulkRemoveModelsUseCase = resolve()

// Local Model Files
fun KoinHelper.getObserveModelDirectoriesUseCase(): ObserveModelDirectoriesUseCase = resolve()
fun KoinHelper.getAddModelDirectoryUseCase(): AddModelDirectoryUseCase = resolve()
fun KoinHelper.getRemoveModelDirectoryUseCase(): RemoveModelDirectoryUseCase = resolve()
fun KoinHelper.getObserveLocalModelFilesUseCase(): ObserveLocalModelFilesUseCase = resolve()
fun KoinHelper.getScanModelDirectoriesUseCase(): ScanModelDirectoriesUseCase = resolve()
fun KoinHelper.getVerifyModelHashUseCase(): VerifyModelHashUseCase = resolve()
fun KoinHelper.getObserveOwnedModelHashesUseCase(): ObserveOwnedModelHashesUseCase = resolve()

// Dataset
fun KoinHelper.getObserveDatasetCollectionsUseCase(): ObserveDatasetCollectionsUseCase = resolve()
fun KoinHelper.getCreateDatasetCollectionUseCase(): CreateDatasetCollectionUseCase = resolve()
fun KoinHelper.getRenameDatasetCollectionUseCase(): RenameDatasetCollectionUseCase = resolve()
fun KoinHelper.getDeleteDatasetCollectionUseCase(): DeleteDatasetCollectionUseCase = resolve()
fun KoinHelper.getObserveDatasetImagesUseCase(): ObserveDatasetImagesUseCase = resolve()
fun KoinHelper.getAddImageToDatasetUseCase(): AddImageToDatasetUseCase = resolve()
fun KoinHelper.getRemoveImageFromDatasetUseCase(): RemoveImageFromDatasetUseCase = resolve()
fun KoinHelper.getBatchEditTagsUseCase(): BatchEditTagsUseCase = resolve()
fun KoinHelper.getEditCaptionUseCase(): EditCaptionUseCase = resolve()
fun KoinHelper.getGetTagSuggestionsUseCase(): GetTagSuggestionsUseCase = resolve()
fun KoinHelper.getUpdateTrainableUseCase(): UpdateTrainableUseCase = resolve()
fun KoinHelper.getGetNonTrainableImagesUseCase(): GetNonTrainableImagesUseCase = resolve()
fun KoinHelper.getDetectDuplicatesUseCase(): DetectDuplicatesUseCase = resolve()
fun KoinHelper.getFilterByResolutionUseCase(): FilterByResolutionUseCase = resolve()
fun KoinHelper.getMarkImageExcludedUseCase(): MarkImageExcludedUseCase = resolve()
fun KoinHelper.getStorePHashUseCase(): StorePHashUseCase = resolve()
fun KoinHelper.getStoreImageDimensionsUseCase(): StoreImageDimensionsUseCase = resolve()
fun KoinHelper.getExportDatasetUseCase(): ExportDatasetUseCase = resolve()
fun KoinHelper.getGetAvailableExportFormatsUseCase(): GetAvailableExportFormatsUseCase = resolve()
fun KoinHelper.getExportWithPluginUseCase(): ExportWithPluginUseCase = resolve()
