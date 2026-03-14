package com.riox432.civitdeck.ui

/**
 * Sealed hierarchy of routes for Desktop navigation.
 * Managed via a simple backstack (mutableStateListOf).
 */
sealed class DesktopRoute {
    data object SearchList : DesktopRoute()
    data class ModelDetail(val modelId: Long) : DesktopRoute()
    data class ImageViewer(
        val imageUrls: List<String>,
        val initialIndex: Int = 0,
    ) : DesktopRoute()
    data class CreatorProfile(val username: String) : DesktopRoute()
}
