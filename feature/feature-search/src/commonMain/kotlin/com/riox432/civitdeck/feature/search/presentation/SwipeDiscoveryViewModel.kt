package com.riox432.civitdeck.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.feature.search.domain.usecase.GetDiscoveryModelsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SwipeDiscoveryState(
    val cards: List<Model> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastDismissed: DismissedCard? = null,
)

data class DismissedCard(
    val model: Model,
    val wasFavorited: Boolean,
)

class SwipeDiscoveryViewModel(
    private val getDiscoveryModels: GetDiscoveryModelsUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SwipeDiscoveryState())
    val state: StateFlow<SwipeDiscoveryState> = _state.asStateFlow()

    private val prefetchThreshold = 3
    private val dismissedIds = mutableSetOf<Long>()

    init {
        loadModels()
    }

    fun loadModels() {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val models = getDiscoveryModels()
                _state.update { current ->
                    val existingIds = current.cards.map { it.id }.toSet()
                    val allSeenIds = existingIds + dismissedIds
                    val newModels = models.filterNot { it.id in allSeenIds }
                    current.copy(cards = current.cards + newModels, isLoading = false)
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onSwipeRight(model: Model) {
        removeTopCard(model, wasFavorited = true)
        viewModelScope.launch { toggleFavorite(model) }
    }

    fun onSwipeLeft(model: Model) {
        removeTopCard(model, wasFavorited = false)
    }

    fun onSwipeUp(model: Model): Long {
        removeTopCard(model, wasFavorited = false)
        return model.id
    }

    fun undoLastSwipe() {
        val dismissed = _state.value.lastDismissed ?: return
        _state.update {
            it.copy(
                cards = listOf(dismissed.model) + it.cards,
                lastDismissed = null,
            )
        }
        if (dismissed.wasFavorited) {
            viewModelScope.launch { toggleFavorite(dismissed.model) }
        }
    }

    private fun removeTopCard(model: Model, wasFavorited: Boolean) {
        dismissedIds.add(model.id)
        _state.update {
            it.copy(
                cards = it.cards.filterNot { card -> card.id == model.id },
                lastDismissed = DismissedCard(model, wasFavorited),
            )
        }
        if (_state.value.cards.size <= prefetchThreshold) {
            loadModels()
        }
    }
}
