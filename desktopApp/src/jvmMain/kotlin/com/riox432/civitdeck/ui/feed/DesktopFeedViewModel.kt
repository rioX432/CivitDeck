package com.riox432.civitdeck.ui.feed

import com.riox432.civitdeck.domain.model.FeedItem
import com.riox432.civitdeck.domain.usecase.GetCreatorFeedUseCase
import com.riox432.civitdeck.domain.usecase.MarkFeedReadUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.util.suspendRunCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DesktopFeedUiState(
    val feedItems: List<FeedItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

class DesktopFeedViewModel(
    private val getCreatorFeedUseCase: GetCreatorFeedUseCase,
    private val markFeedReadUseCase: MarkFeedReadUseCase,
) : ViewModel() {

    private val scope = viewModelScope

    private val _uiState = MutableStateFlow(DesktopFeedUiState())
    val uiState: StateFlow<DesktopFeedUiState> = _uiState

    init {
        loadFeed()
    }

    fun refresh() {
        loadFeed(forceRefresh = true)
    }

    private fun loadFeed(forceRefresh: Boolean = false) {
        scope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !forceRefresh && it.feedItems.isEmpty(),
                    isRefreshing = forceRefresh,
                    error = null,
                )
            }
            suspendRunCatching { getCreatorFeedUseCase(forceRefresh) }
                .onSuccess { items ->
                    _uiState.update {
                        it.copy(
                            feedItems = items,
                            isLoading = false,
                            isRefreshing = false,
                        )
                    }
                    if (items.isNotEmpty()) markFeedReadUseCase()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = e.message ?: "Failed to load feed",
                        )
                    }
                }
        }
    }

    public override fun onCleared() {
        super.onCleared()
    }
}
