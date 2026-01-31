package com.omooooori.civitdeck.di

import com.omooooori.civitdeck.domain.usecase.GetImagesUseCase
import com.omooooori.civitdeck.domain.usecase.GetModelDetailUseCase
import com.omooooori.civitdeck.domain.usecase.GetModelsUseCase
import com.omooooori.civitdeck.domain.usecase.ObserveFavoritesUseCase
import com.omooooori.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.omooooori.civitdeck.domain.usecase.ToggleFavoriteUseCase
import org.koin.mp.KoinPlatform.getKoin

object KoinHelper {
    fun getModelsUseCase(): GetModelsUseCase = getKoin().get()
    fun getModelDetailUseCase(): GetModelDetailUseCase = getKoin().get()
    fun getImagesUseCase(): GetImagesUseCase = getKoin().get()
    fun getToggleFavoriteUseCase(): ToggleFavoriteUseCase = getKoin().get()
    fun getObserveFavoritesUseCase(): ObserveFavoritesUseCase = getKoin().get()
    fun getObserveIsFavoriteUseCase(): ObserveIsFavoriteUseCase = getKoin().get()
}
