package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.ml.ImageEmbeddingModel
import com.riox432.civitdeck.domain.ml.SigLIP2Bridge
import com.riox432.civitdeck.domain.repository.ModelEmbeddingRepository
import com.riox432.civitdeck.domain.usecase.AddExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.EmbedImageUseCase
import com.riox432.civitdeck.domain.usecase.EmbedOnBrowseUseCase
import com.riox432.civitdeck.domain.usecase.EnrichModelImagesUseCase
import com.riox432.civitdeck.domain.usecase.FindSimilarModelsByEmbeddingUseCase
import com.riox432.civitdeck.domain.usecase.GetExcludedTagsUseCase
import com.riox432.civitdeck.domain.usecase.RemoveExcludedTagUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.AddSearchHistoryUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.DeleteSavedSearchFilterUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.DeleteSearchHistoryItemUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetDiscoveryModelsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetRecommendationsUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.MultiSourceSearchUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.ObserveSavedSearchFiltersUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.ObserveSearchHistoryUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.SaveSearchFilterUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.TrackRecommendationClickUseCase
import com.riox432.civitdeck.domain.ml.registerSigLIP2Bridge as registerSigLIP2BridgeImpl

// Search & Discovery
fun KoinHelper.getModelsUseCase(): GetModelsUseCase = resolve()
fun KoinHelper.getMultiSourceSearchUseCase(): MultiSourceSearchUseCase = resolve()
fun KoinHelper.getDiscoveryModelsUseCase(): GetDiscoveryModelsUseCase = resolve()
fun KoinHelper.getRecommendationsUseCase(): GetRecommendationsUseCase = resolve()
fun KoinHelper.getTrackRecommendationClickUseCase(): TrackRecommendationClickUseCase = resolve()
fun KoinHelper.getEnrichModelImagesUseCase(): EnrichModelImagesUseCase = resolve()

// Image Embedding (#700)
fun KoinHelper.getImageEmbeddingModel(): ImageEmbeddingModel = resolve()
fun KoinHelper.getEmbedImageUseCase(): EmbedImageUseCase = resolve()
fun KoinHelper.getEmbedOnBrowseUseCase(): EmbedOnBrowseUseCase = resolve()
fun KoinHelper.getFindSimilarModelsByEmbeddingUseCase(): FindSimilarModelsByEmbeddingUseCase = resolve()
fun KoinHelper.getModelEmbeddingRepository(): ModelEmbeddingRepository = resolve()

/**
 * Registers the Swift Core ML bridge so the iOS [ImageEmbeddingModel] actual can use it.
 * Called once from `iOSApp.swift` after `doInitKoin`.
 */
fun KoinHelper.registerSigLIP2Bridge(bridge: SigLIP2Bridge) {
    registerSigLIP2BridgeImpl(bridge)
}

// Search History & Filters
fun KoinHelper.getObserveSearchHistoryUseCase(): ObserveSearchHistoryUseCase = resolve()
fun KoinHelper.getAddSearchHistoryUseCase(): AddSearchHistoryUseCase = resolve()
fun KoinHelper.getClearSearchHistoryUseCase(): ClearSearchHistoryUseCase = resolve()
fun KoinHelper.getDeleteSearchHistoryItemUseCase(): DeleteSearchHistoryItemUseCase = resolve()
fun KoinHelper.getObserveSavedSearchFiltersUseCase(): ObserveSavedSearchFiltersUseCase = resolve()
fun KoinHelper.getSaveSearchFilterUseCase(): SaveSearchFilterUseCase = resolve()
fun KoinHelper.getDeleteSavedSearchFilterUseCase(): DeleteSavedSearchFilterUseCase = resolve()
fun KoinHelper.getExcludedTagsUseCase(): GetExcludedTagsUseCase = resolve()
fun KoinHelper.getAddExcludedTagUseCase(): AddExcludedTagUseCase = resolve()
fun KoinHelper.getRemoveExcludedTagUseCase(): RemoveExcludedTagUseCase = resolve()
