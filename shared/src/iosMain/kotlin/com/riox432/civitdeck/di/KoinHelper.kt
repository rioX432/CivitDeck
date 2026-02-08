package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.AddSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.DeleteSavedPromptUseCase
import com.riox432.civitdeck.domain.usecase.GetCreatorModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetImagesUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.GetModelsUseCase
import com.riox432.civitdeck.domain.usecase.GetRecommendationsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveFavoritesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObserveSavedPromptsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.SavePromptUseCase
import com.riox432.civitdeck.domain.usecase.SetNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
import org.koin.mp.KoinPlatform.getKoin

@Suppress("TooManyFunctions")
object KoinHelper {
    fun getModelsUseCase(): GetModelsUseCase = getKoin().get()
    fun getCreatorModelsUseCase(): GetCreatorModelsUseCase = getKoin().get()
    fun getModelDetailUseCase(): GetModelDetailUseCase = getKoin().get()
    fun getImagesUseCase(): GetImagesUseCase = getKoin().get()
    fun getToggleFavoriteUseCase(): ToggleFavoriteUseCase = getKoin().get()
    fun getObserveFavoritesUseCase(): ObserveFavoritesUseCase = getKoin().get()
    fun getObserveIsFavoriteUseCase(): ObserveIsFavoriteUseCase = getKoin().get()
    fun getObserveNsfwFilterUseCase(): ObserveNsfwFilterUseCase = getKoin().get()
    fun getSetNsfwFilterUseCase(): SetNsfwFilterUseCase = getKoin().get()
    fun getSavePromptUseCase(): SavePromptUseCase = getKoin().get()
    fun getObserveSavedPromptsUseCase(): ObserveSavedPromptsUseCase = getKoin().get()
    fun getDeleteSavedPromptUseCase(): DeleteSavedPromptUseCase = getKoin().get()
    fun getObserveSearchHistoryUseCase(): ObserveSearchHistoryUseCase = getKoin().get()
    fun getAddSearchHistoryUseCase(): AddSearchHistoryUseCase = getKoin().get()
    fun getClearSearchHistoryUseCase(): ClearSearchHistoryUseCase = getKoin().get()
    fun getTrackModelViewUseCase(): TrackModelViewUseCase = getKoin().get()
    fun getRecommendationsUseCase(): GetRecommendationsUseCase = getKoin().get()
}
