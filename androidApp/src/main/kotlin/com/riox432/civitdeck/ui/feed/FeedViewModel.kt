package com.riox432.civitdeck.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.FeedItem
import com.riox432.civitdeck.domain.usecase.GetCreatorFeedUseCase
import com.riox432.civitdeck.domain.usecase.GetUnreadFeedCountUseCase
import com.riox432.civitdeck.domain.usecase.MarkFeedReadUseCase
import com.riox432.civitdeck.domain.usecase.ObserveQualityThresholdUseCase
import com.riox432.civitdeck.domain.usecase.QualityScoreCalculator
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
    private val observeQualityThresholdUseCase: ObserveQualityThresholdUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState

    private var allItems: List<FeedItem> = emptyList()
    private var qualityThreshold: Int = 0

    init {
        observeQualityThreshold()
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
                allItems = getCreatorFeedUseCase(forceRefresh)
                _uiState.value = _uiState.value.copy(
                    feedItems = filterByQuality(allItems),
                    isLoading = false,
                    isRefreshing = false,
                )
                if (allItems.isNotEmpty()) markFeedReadUseCase()
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

    private fun observeQualityThreshold() {
        viewModelScope.launch {
            observeQualityThresholdUseCase().collect { threshold ->
                qualityThreshold = threshold
                if (allItems.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        feedItems = filterByQuality(allItems),
                    )
                }
            }
        }
    }

    private fun filterByQuality(items: List<FeedItem>): List<FeedItem> {
        if (qualityThreshold <= 0) return items
        return items.filter { item ->
            QualityScoreCalculator.calculate(item.stats) >= qualityThreshold
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
