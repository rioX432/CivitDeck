package com.riox432.civitdeck.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.FeedItem
import com.riox432.civitdeck.domain.usecase.GetCreatorFeedUseCase
import com.riox432.civitdeck.domain.usecase.GetUnreadFeedCountUseCase
import com.riox432.civitdeck.domain.usecase.MarkFeedReadUseCase
import com.riox432.civitdeck.domain.util.suspendRunCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedUiState(
    val feedItems: List<FeedItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val unreadCount: Int = 0,
)

class FeedViewModel(
    private val getCreatorFeedUseCase: GetCreatorFeedUseCase,
    private val getUnreadFeedCountUseCase: GetUnreadFeedCountUseCase,
    private val markFeedReadUseCase: MarkFeedReadUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState

    init {
        loadFeed()
        observeUnreadCount()
    }

    fun refresh() {
        loadFeed(forceRefresh = true)
    }

    fun markAsRead() {
        viewModelScope.launch {
            markFeedReadUseCase()
        }
    }

    private fun loadFeed(forceRefresh: Boolean = false) {
        viewModelScope.launch {
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

    private fun observeUnreadCount() {
        viewModelScope.launch {
            getUnreadFeedCountUseCase().collect { count ->
                _uiState.update { it.copy(unreadCount = count) }
            }
        }
    }
}
