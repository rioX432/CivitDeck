package com.riox432.civitdeck.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.FeedItem
import com.riox432.civitdeck.domain.usecase.GetCreatorFeedUseCase
import com.riox432.civitdeck.domain.usecase.GetUnreadFeedCountUseCase
import com.riox432.civitdeck.domain.usecase.MarkFeedReadUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
            _uiState.value = _uiState.value.copy(
                isLoading = !forceRefresh && _uiState.value.feedItems.isEmpty(),
                isRefreshing = forceRefresh,
                error = null,
            )
            try {
                val items = getCreatorFeedUseCase(forceRefresh)
                _uiState.value = _uiState.value.copy(
                    feedItems = items,
                    isLoading = false,
                    isRefreshing = false,
                )
                if (items.isNotEmpty()) markFeedReadUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message ?: "Failed to load feed",
                )
            }
        }
    }

    private fun observeUnreadCount() {
        viewModelScope.launch {
            getUnreadFeedCountUseCase().collect { count ->
                _uiState.value = _uiState.value.copy(unreadCount = count)
            }
        }
    }
}
