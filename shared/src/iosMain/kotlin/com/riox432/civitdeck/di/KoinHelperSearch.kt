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
import org.koin.mp.KoinPlatform.getKoin
import com.riox432.civitdeck.domain.ml.registerSigLIP2Bridge as registerSigLIP2BridgeImpl

// Search & Discovery
fun KoinHelper.getModelsUseCase(): GetModelsUseCase = getKoin().get()
fun KoinHelper.getMultiSourceSearchUseCase(): MultiSourceSearchUseCase = getKoin().get()
fun KoinHelper.getDiscoveryModelsUseCase(): GetDiscoveryModelsUseCase = getKoin().get()
fun KoinHelper.getRecommendationsUseCase(): GetRecommendationsUseCase = getKoin().get()
fun KoinHelper.getTrackRecommendationClickUseCase(): TrackRecommendationClickUseCase = getKoin().get()
fun KoinHelper.getEnrichModelImagesUseCase(): EnrichModelImagesUseCase = getKoin().get()

// Image Embedding (#700)
fun KoinHelper.getImageEmbeddingModel(): ImageEmbeddingModel = getKoin().get()
fun KoinHelper.getEmbedImageUseCase(): EmbedImageUseCase = getKoin().get()
fun KoinHelper.getEmbedOnBrowseUseCase(): EmbedOnBrowseUseCase = getKoin().get()
fun KoinHelper.getFindSimilarModelsByEmbeddingUseCase(): FindSimilarModelsByEmbeddingUseCase = getKoin().get()
fun KoinHelper.getModelEmbeddingRepository(): ModelEmbeddingRepository = getKoin().get()

/**
 * Registers the Swift Core ML bridge so the iOS [ImageEmbeddingModel] actual can use it.
 * Called once from `iOSApp.swift` after `doInitKoin`.
 */
fun KoinHelper.registerSigLIP2Bridge(bridge: SigLIP2Bridge) {
    registerSigLIP2BridgeImpl(bridge)
}

// Search History & Filters
fun KoinHelper.getObserveSearchHistoryUseCase(): ObserveSearchHistoryUseCase = getKoin().get()
fun KoinHelper.getAddSearchHistoryUseCase(): AddSearchHistoryUseCase = getKoin().get()
fun KoinHelper.getClearSearchHistoryUseCase(): ClearSearchHistoryUseCase = getKoin().get()
fun KoinHelper.getDeleteSearchHistoryItemUseCase(): DeleteSearchHistoryItemUseCase = getKoin().get()
fun KoinHelper.getObserveSavedSearchFiltersUseCase(): ObserveSavedSearchFiltersUseCase = getKoin().get()
fun KoinHelper.getSaveSearchFilterUseCase(): SaveSearchFilterUseCase = getKoin().get()
fun KoinHelper.getDeleteSavedSearchFilterUseCase(): DeleteSavedSearchFilterUseCase = getKoin().get()
fun KoinHelper.getExcludedTagsUseCase(): GetExcludedTagsUseCase = getKoin().get()
fun KoinHelper.getAddExcludedTagUseCase(): AddExcludedTagUseCase = getKoin().get()
fun KoinHelper.getRemoveExcludedTagUseCase(): RemoveExcludedTagUseCase = getKoin().get()
