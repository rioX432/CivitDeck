package com.riox432.civitdeck.domain.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Reusable helper for cursor-based paginated loading.
 *
 * Handles: guard checks, loading state management, cursor tracking,
 * appending results, and error handling with CancellationException rethrow.
 */
class PaginatedLoader<T>(
    private val scope: CoroutineScope,
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
    private val load: suspend (cursor: String?, limit: Int) -> LoadResult<T>,
    private val onStateChanged: (PaginatedLoadState<T>) -> Unit,
) {
    private var job: Job? = null
    private val _state = MutableStateFlow(PaginatedLoadState<T>())

    val state: PaginatedLoadState<T> get() = _state.value

    /**
     * Load the first page. Cancels any in-progress load and resets state.
     */
    fun loadFirst() {
        job?.cancel()
        _state.update {
            PaginatedLoadState(isLoading = true)
        }
        notifyState()
        doLoad(isLoadMore = false)
    }

    /**
     * Load the next page. No-op if already loading or no more pages.
     */
    fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || !current.hasMore) return
        _state.update { it.copy(isLoadingMore = true) }
        notifyState()
        doLoad(isLoadMore = true)
    }

    /**
     * Cancel any in-progress load.
     */
    fun cancel() {
        job?.cancel()
    }

    private fun doLoad(isLoadMore: Boolean) {
        job = scope.launch {
            val cursor = if (isLoadMore) _state.value.nextCursor else null
            suspendRunCatching { load(cursor, pageSize) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            items = if (isLoadMore) it.items + result.items else result.items,
                            isLoading = false,
                            isLoadingMore = false,
                            error = null,
                            nextCursor = result.nextCursor,
                            hasMore = result.nextCursor != null,
                        )
                    }
                    notifyState()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = e.message ?: "Unknown error",
                        )
                    }
                    notifyState()
                }
        }
    }

    private fun notifyState() {
        onStateChanged(_state.value)
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
    }
}

/**
 * Immutable state snapshot for paginated loading.
 */
data class PaginatedLoadState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = true,
)

/**
 * Result of a single page load.
 */
data class LoadResult<T>(
    val items: List<T>,
    val nextCursor: String?,
)
