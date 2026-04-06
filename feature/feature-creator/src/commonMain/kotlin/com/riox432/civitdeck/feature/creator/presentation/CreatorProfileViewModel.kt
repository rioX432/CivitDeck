package com.riox432.civitdeck.feature.creator.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.usecase.FollowCreatorUseCase
import com.riox432.civitdeck.domain.usecase.IsFollowingCreatorUseCase
import com.riox432.civitdeck.domain.usecase.UnfollowCreatorUseCase
import com.riox432.civitdeck.domain.util.LoadResult
import com.riox432.civitdeck.domain.util.PaginatedLoader
import com.riox432.civitdeck.feature.creator.domain.usecase.GetCreatorModelsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreatorProfileUiState(
    val username: String = "",
    val models: List<Model> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val isFollowing: Boolean = false,
    val error: String? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = true,
)

class CreatorProfileViewModel(
    private val username: String,
    private val getCreatorModelsUseCase: GetCreatorModelsUseCase,
    private val isFollowingCreatorUseCase: IsFollowingCreatorUseCase,
    private val followCreatorUseCase: FollowCreatorUseCase,
    private val unfollowCreatorUseCase: UnfollowCreatorUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatorProfileUiState(username = username))
    val uiState: StateFlow<CreatorProfileUiState> = _uiState.asStateFlow()

    private val paginatedLoader = PaginatedLoader<Model>(
        scope = viewModelScope,
        pageSize = PAGE_SIZE,
        load = { cursor, limit -> loadPage(cursor, limit) },
        onStateChanged = { loadState ->
            _uiState.update {
                it.copy(
                    models = loadState.items,
                    isLoading = loadState.isLoading,
                    isLoadingMore = loadState.isLoadingMore,
                    error = loadState.error,
                    nextCursor = loadState.nextCursor,
                    hasMore = loadState.hasMore,
                )
            }
        },
    )

    init {
        paginatedLoader.loadFirst()
        observeFollowState()
    }

    fun loadMore() {
        paginatedLoader.loadMore()
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        paginatedLoader.loadFirst()
    }

    fun toggleFollow() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.isFollowing) {
                unfollowCreatorUseCase(username)
            } else {
                val creator = state.models.firstOrNull()?.creator
                followCreatorUseCase(
                    username = username,
                    displayName = creator?.username ?: username,
                    avatarUrl = creator?.image,
                )
            }
        }
    }

    private fun observeFollowState() {
        viewModelScope.launch {
            isFollowingCreatorUseCase(username).collect { following ->
                _uiState.update { it.copy(isFollowing = following) }
            }
        }
    }

    private suspend fun loadPage(cursor: String?, limit: Int): LoadResult<Model> {
        val result = getCreatorModelsUseCase(
            username = username,
            cursor = cursor,
            limit = limit,
        )
        // Clear refreshing flag when load completes
        _uiState.update { it.copy(isRefreshing = false) }
        return LoadResult(
            items = result.items,
            nextCursor = result.metadata.nextCursor,
        )
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
