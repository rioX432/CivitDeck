package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.repository.FavoriteRepository

class ToggleFavoriteUseCase(private val repository: FavoriteRepository) {
    suspend operator fun invoke(model: Model) = repository.toggleFavorite(model)
}
