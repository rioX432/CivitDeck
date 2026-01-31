package com.omooooori.civitdeck.domain.usecase

import com.omooooori.civitdeck.domain.model.Model
import com.omooooori.civitdeck.domain.repository.FavoriteRepository

class ToggleFavoriteUseCase(private val repository: FavoriteRepository) {
    suspend operator fun invoke(model: Model) = repository.toggleFavorite(model)
}
