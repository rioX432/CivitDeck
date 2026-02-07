package com.riox432.civitdeck.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.FavoriteModelSummary
import com.riox432.civitdeck.domain.usecase.ObserveFavoritesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FavoritesViewModel(
    observeFavoritesUseCase: ObserveFavoritesUseCase,
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteModelSummary>> =
        observeFavoritesUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
