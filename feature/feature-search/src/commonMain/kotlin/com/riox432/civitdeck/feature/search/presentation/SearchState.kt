package com.riox432.civitdeck.feature.search.presentation

/**
 * High-level phase of the unified Discover search bar. Personalized recommendations are
 * shown only in [Idle]; both [Editing] and [Results] hide them so what the user is looking
 * for comes first, and they are restored the moment the bar returns to [Idle].
 *
 * Derived from the flat [ModelSearchUiState] snapshot rather than being the state itself,
 * so the iOS SKIE wrapper can keep observing individual fields.
 */
sealed interface SearchState {
    /** No committed search and the bar is not focused — recommendations are visible. */
    data object Idle : SearchState

    /** The bar is focused and the user is typing, but nothing has been committed yet. */
    data object Editing : SearchState

    /** A keyword/tag, semantic, image or find-similar search is showing results. */
    data object Results : SearchState
}
