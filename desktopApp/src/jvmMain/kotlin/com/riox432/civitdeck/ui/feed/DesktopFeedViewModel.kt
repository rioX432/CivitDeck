package com.riox432.civitdeck.ui.feed

import com.riox432.civitdeck.domain.model.FeedItem
import com.riox432.civitdeck.domain.usecase.GetCreatorFeedUseCase
import com.riox432.civitdeck.domain.usecase.MarkFeedReadUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    public override fun onCleared() {
        super.onCleared()
    }
}
