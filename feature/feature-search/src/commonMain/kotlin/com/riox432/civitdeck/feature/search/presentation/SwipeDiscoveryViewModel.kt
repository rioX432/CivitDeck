package com.riox432.civitdeck.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.filterNsfwImages
import com.riox432.civitdeck.domain.model.includeNsfwModels
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.util.UiLoadingState
import com.riox432.civitdeck.feature.search.domain.usecase.GetDiscoveryModelsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SwipeDiscoveryState(
    val cards: List<Model> = emptyList(),
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val lastDismissed: DismissedCard? = null,
) : UiLoadingState

data class DismissedCard(
    val model: Model,
    val wasFavorited: Boolean,
)

class SwipeDiscoveryViewModel(
    private val getDiscoveryModels: GetDiscoveryModelsUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val observeNsfwFilter: ObserveNsfwFilterUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SwipeDiscoveryState())
    val state: StateFlow<SwipeDiscoveryState> = _state.asStateFlow()

    private val prefetchThreshold = 3

    private var nextCursor: String? = null
    private var hasMore = true

    // A cursor only continues the query it was issued for, so paging restarts when the
    // `nsfw` request flag changes.
    private var cursorNsfw: Boolean? = null

    companion object {
        /** Persists dismissed model IDs across ViewModel recreations within the same session. */
        private val sessionDismissedIds = MutableStateFlow<Set<Long>>(emptySet())

        /** Same bound as `SearchPageLoader`: pages fetched per load while every result is already seen. */
        private const val MAX_FETCH_ITERATIONS = 5
    }

    init {
        loadModels()
    }

    fun loadModels() {
        if (_state.value.isLoading) return
        // Set before launching so a swipe-triggered prefetch cannot start a second load
        // with the same cursor before this one runs.
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val nsfwLevel = observeNsfwFilter().first()
                val nsfw = nsfwLevel.includeNsfwModels()
                if (nsfw != cursorNsfw) {
                    cursorNsfw = nsfw
                    nextCursor = null
                    hasMore = true
                }
                val newModels = if (hasMore) fetchUnseenModels(nsfwLevel) else emptyList()
                _state.update { current ->
                    val existingIds = current.cards.map { it.id }.toSet()
                    val added = newModels.filterNot { it.id in existingIds }
                    current.copy(cards = current.cards + added, isLoading = false)
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun fetchUnseenModels(nsfwLevel: NsfwFilterLevel): List<Model> {
        repeat(MAX_FETCH_ITERATIONS) {
            val page = getDiscoveryModels(nsfw = nsfwLevel.includeNsfwModels(), cursor = nextCursor)
            nextCursor = page.metadata.nextCursor
            hasMore = nextCursor != null
            val seenIds = _state.value.cards.map { it.id }.toSet() + sessionDismissedIds.value
            val unseen = page.items.filterNsfwImages(nsfwLevel).filterNot { it.id in seenIds }
            if (unseen.isNotEmpty() || !hasMore) return unseen
        }
        return emptyList()
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
        sessionDismissedIds.update { it + model.id }
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
