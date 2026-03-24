package com.riox432.civitdeck.ui.navigation

data object SearchRoute

data object CollectionsRoute

data class CollectionDetailRoute(val collectionId: Long, val collectionName: String)

data class DetailRoute(
    val modelId: Long,
    val thumbnailUrl: String? = null,
    val sharedElementSuffix: String = "",
)

data class ImageGalleryRoute(val modelVersionId: Long)

data class CreatorRoute(val username: String)

data object SavedPromptsRoute

data object SettingsRoute

data object LicensesRoute

data object ModelFileBrowserRoute

data object DiscoveryRoute

data class CompareRoute(val leftModelId: Long, val rightModelId: Long)

data object ComfyUISettingsRoute

data object ComfyUIGenerationRoute

data object ComfyUIQueueRoute

data class ComfyUIBridgeRoute(
    val modelId: Long,
    val versionId: Long,
    val sha256Hash: String,
    val modelName: String,
    val prompt: String?,
    val negativePrompt: String?,
    val steps: Int?,
    val cfgScale: Double?,
    val seed: Long?,
    val sampler: String?,
)

data object WorkflowTemplateLibraryRoute

data class WorkflowTemplateEditorRoute(val templateId: Long)

data object WorkflowTemplatePickerRoute

data object SDWebUISettingsRoute

data object SDWebUIGenerationRoute

data object AppearanceSettingsRoute

data object ContentFilterSettingsRoute

data object StorageSettingsRoute

data object AdvancedSettingsRoute

data object CivitaiLinkSettingsRoute

data object ComfyUIHistoryRoute

data class ComfyUIOutputDetailRoute(val imageId: String)

data object BrowseImagesRoute

data object NavShortcutsSettingsRoute

data object ExternalServerSettingsRoute

data object ExternalServerGalleryRoute

data class ExternalServerImageDetailRoute(val imageId: Int)

data object DatasetListRoute

data class DatasetDetailRoute(val datasetId: Long, val datasetName: String)

data class BatchTagEditorRoute(val datasetId: Long)

data class DuplicateReviewRoute(val datasetId: Long)

data object BackupRoute

data object QRScannerRoute

data object AnalyticsRoute

data object BrowsingHistoryRoute

data object ComfyHubBrowserRoute

data class ComfyHubDetailRoute(val workflowId: String)

data object IntegrationsHubRoute

data object PluginManagementRoute

data class PluginDetailRoute(val pluginId: String)

data class SimilarModelsRoute(val modelId: Long)

data object NotificationCenterRoute

data object FeedRoute

data object CreateHubRoute

data object LibraryRoute
