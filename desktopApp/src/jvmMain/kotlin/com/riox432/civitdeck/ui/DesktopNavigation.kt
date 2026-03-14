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
    data class CollectionDetail(val collectionId: Long, val collectionName: String) : DesktopRoute()
    data object DatasetList : DesktopRoute()
    data class DatasetDetail(val datasetId: Long, val datasetName: String) : DesktopRoute()
    data object Backup : DesktopRoute()
    data object PluginList : DesktopRoute()
    data class PluginDetail(val pluginId: String) : DesktopRoute()
    data class ModelCompare(val leftModelId: Long, val rightModelId: Long) : DesktopRoute()
    data object Analytics : DesktopRoute()
    data object QRCode : DesktopRoute()
}
