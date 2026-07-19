package com.riox432.civitdeck.ui.testing

/**
 * Stable `Modifier.testTag` identifiers for discovery-critical UI nodes (issue #990).
 *
 * These strings are the contract shared by three surfaces: Android Compose (`testTag`),
 * Desktop Compose (`testTag`), and the Maestro flows under `.maestro/` (matched with `id:`).
 * iOS mirrors them as accessibility identifiers in `DiscoveryTestTags.swift`. Keep the raw
 * string values in lock-step across all four — a rename here is a breaking change for the QA
 * flows, not a cosmetic edit.
 */
object DiscoveryTestTags {
    /** The unified Discover search text field (#988). */
    const val SEARCH_FIELD = "discovery_search_field"

    /** The scrolling model results grid. */
    const val MODEL_GRID = "discovery_model_grid"

    /** A tappable model card inside the grid. All cards share this tag. */
    const val MODEL_CARD = "discovery_model_card"

    /** Root of the model detail screen. */
    const val MODEL_DETAIL_ROOT = "model_detail_root"

    /** Favorite toggle on the detail screen — the "save to default collection" gesture. */
    const val MODEL_FAVORITE_BUTTON = "model_favorite_button"

    /** "Save prompt" action in the gallery image metadata sheet. */
    const val GALLERY_SAVE_PROMPT = "gallery_save_prompt_button"
}
