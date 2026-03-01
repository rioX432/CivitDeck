import Shared

// MARK: - Core Domain Models
// These typealiases map the module-prefixed KMP type names (after core module split)
// back to short names used throughout the iOS codebase.

typealias AccentColor = Core_domainAccentColor
typealias BaseModel = Core_domainBaseModel
typealias CacheInfo = Core_domainCacheInfo
typealias ComfyUIConnection = Core_domainComfyUIConnection
typealias ComfyUIGenerationParams = Core_domainComfyUIGenerationParams
typealias Creator = Core_domainCreator
typealias FavoriteModelSummary = Core_domainFavoriteModelSummary
typealias GenerationProgress = Core_domainGenerationProgress
typealias GenerationResult = Core_domainGenerationResult
typealias GenerationStatus = Core_domainGenerationStatus
typealias HiddenModel = Core_domainHiddenModel
typealias ImageGenerationMeta = Core_domainImageGenerationMeta
typealias ImageStats = Core_domainImageStats
typealias LocalModelFile = Core_domainLocalModelFile
typealias LoraSelection = Core_domainLoraSelection
typealias MatchedModelInfo = Core_domainMatchedModelInfo
typealias Model = Core_domainModel
typealias ModelCollection = Core_domainModelCollection
typealias ModelDirectory = Core_domainModelDirectory
typealias ModelFile = Core_domainModelFile
typealias ModelImage = Core_domainModelImage
typealias ModelMode = Core_domainModelMode
typealias ThemeMode = Core_domainThemeMode
typealias ModelStats = Core_domainModelStats
typealias ModelType = Core_domainModelType
typealias ModelUpdate = Core_domainModelUpdate
typealias ModelVersion = Core_domainModelVersion
typealias ModelVersionStats = Core_domainModelVersionStats
typealias CivitaiLinkActivity = Core_domainCivitaiLinkActivity
typealias CivitaiLinkStatus = Core_domainCivitaiLinkStatus
typealias NsfwBlurSettings = Core_domainNsfwBlurSettings
typealias NsfwFilterLevel = Core_domainNsfwFilterLevel
typealias NsfwLevel = Core_domainNsfwLevel
typealias PageMetadata = Core_domainPageMetadata
typealias PaginatedResult = Core_domainPaginatedResult
typealias PollingInterval = Core_domainPollingInterval
typealias QueueJob = Core_domainQueueJob
typealias QueueJobStatus = Core_domainQueueJobStatus
typealias RecommendationSection = Core_domainRecommendationSection
typealias SavedPrompt = Core_domainSavedPrompt
typealias CivitaiLinkResource = Core_domainCivitaiLinkResource
typealias SDWebUIConnection = Core_domainSDWebUIConnection
typealias SDWebUIGenerationParams = Core_domainSDWebUIGenerationParams
typealias SDWebUIGenerationProgressGenerating = Core_domainSDWebUIGenerationProgress.Generating
typealias SDWebUIGenerationProgressCompleted = Core_domainSDWebUIGenerationProgress.Completed
typealias SDWebUIGenerationProgressError = Core_domainSDWebUIGenerationProgress.Error
typealias SortOrder = Core_domainSortOrder
typealias Tag = Core_domainTag
typealias TimePeriod = Core_domainTimePeriod

// MARK: - Core Domain Use Cases

typealias AddModelDirectoryUseCase = Core_domainAddModelDirectoryUseCase
typealias CheckModelUpdatesUseCase = Core_domainCheckModelUpdatesUseCase
typealias ClearBrowsingHistoryUseCase = Core_domainClearBrowsingHistoryUseCase
typealias ClearCacheUseCase = Core_domainClearCacheUseCase
typealias EvictCacheUseCase = Core_domainEvictCacheUseCase
typealias GetCacheInfoUseCase = Core_domainGetCacheInfoUseCase
typealias GetHiddenModelsUseCase = Core_domainGetHiddenModelsUseCase
typealias GetModelDetailUseCase = Core_domainGetModelDetailUseCase
typealias GetViewedModelIdsUseCase = Core_domainGetViewedModelIdsUseCase
typealias ObserveAccentColorUseCase = Core_domainObserveAccentColorUseCase
typealias ObserveThemeModeUseCase = Core_domainObserveThemeModeUseCase
typealias ObserveAmoledDarkModeUseCase = Core_domainObserveAmoledDarkModeUseCase
typealias ObserveApiKeyUseCase = Core_domainObserveApiKeyUseCase
typealias ObserveCacheSizeLimitUseCase = Core_domainObserveCacheSizeLimitUseCase
typealias ObserveDefaultSortOrderUseCase = Core_domainObserveDefaultSortOrderUseCase
typealias ObserveDefaultTimePeriodUseCase = Core_domainObserveDefaultTimePeriodUseCase
typealias ObserveFavoritesUseCase = Core_domainObserveFavoritesUseCase
typealias ObserveGridColumnsUseCase = Core_domainObserveGridColumnsUseCase
typealias ObserveIsFavoriteUseCase = Core_domainObserveIsFavoriteUseCase
typealias ObserveLocalModelFilesUseCase = Core_domainObserveLocalModelFilesUseCase
typealias ObserveModelDirectoriesUseCase = Core_domainObserveModelDirectoriesUseCase
typealias ObserveNetworkStatusUseCase = Core_domainObserveNetworkStatusUseCase
typealias ObserveNotificationsEnabledUseCase = Core_domainObserveNotificationsEnabledUseCase
typealias ObserveNsfwBlurSettingsUseCase = Core_domainObserveNsfwBlurSettingsUseCase
typealias ObserveNsfwFilterUseCase = Core_domainObserveNsfwFilterUseCase
typealias ObserveOfflineCacheEnabledUseCase = Core_domainObserveOfflineCacheEnabledUseCase
typealias ObserveOwnedModelHashesUseCase = Core_domainObserveOwnedModelHashesUseCase
typealias ObservePollingIntervalUseCase = Core_domainObservePollingIntervalUseCase
typealias ObservePowerUserModeUseCase = Core_domainObservePowerUserModeUseCase
typealias ObserveSeenTutorialVersionUseCase = Core_domainObserveSeenTutorialVersionUseCase
typealias RemoveModelDirectoryUseCase = Core_domainRemoveModelDirectoryUseCase
typealias ScanModelDirectoriesUseCase = Core_domainScanModelDirectoriesUseCase
typealias SetAccentColorUseCase = Core_domainSetAccentColorUseCase
typealias SetAmoledDarkModeUseCase = Core_domainSetAmoledDarkModeUseCase
typealias SetApiKeyUseCase = Core_domainSetApiKeyUseCase
typealias SetCacheSizeLimitUseCase = Core_domainSetCacheSizeLimitUseCase
typealias SetDefaultSortOrderUseCase = Core_domainSetDefaultSortOrderUseCase
typealias SetDefaultTimePeriodUseCase = Core_domainSetDefaultTimePeriodUseCase
typealias SetGridColumnsUseCase = Core_domainSetGridColumnsUseCase
typealias SetNotificationsEnabledUseCase = Core_domainSetNotificationsEnabledUseCase
typealias SetNsfwBlurSettingsUseCase = Core_domainSetNsfwBlurSettingsUseCase
typealias SetNsfwFilterUseCase = Core_domainSetNsfwFilterUseCase
typealias SetOfflineCacheEnabledUseCase = Core_domainSetOfflineCacheEnabledUseCase
typealias SetPollingIntervalUseCase = Core_domainSetPollingIntervalUseCase
typealias SetPowerUserModeUseCase = Core_domainSetPowerUserModeUseCase
typealias SetSeenTutorialVersionUseCase = Core_domainSetSeenTutorialVersionUseCase
typealias ToggleFavoriteUseCase = Core_domainToggleFavoriteUseCase
typealias TrackModelViewUseCase = Core_domainTrackModelViewUseCase
typealias ValidateApiKeyUseCase = Core_domainValidateApiKeyUseCase
typealias VerifyModelHashUseCase = Core_domainVerifyModelHashUseCase

// MARK: - Feature: Collections

typealias AddModelToCollectionUseCase = Feature_collectionsAddModelToCollectionUseCase
typealias BulkMoveModelsUseCase = Feature_collectionsBulkMoveModelsUseCase
typealias BulkRemoveModelsUseCase = Feature_collectionsBulkRemoveModelsUseCase
typealias CreateCollectionUseCase = Feature_collectionsCreateCollectionUseCase
typealias DeleteCollectionUseCase = Feature_collectionsDeleteCollectionUseCase
typealias ObserveCollectionModelsUseCase = Feature_collectionsObserveCollectionModelsUseCase
typealias ObserveCollectionsUseCase = Feature_collectionsObserveCollectionsUseCase
typealias ObserveModelCollectionsUseCase = Feature_collectionsObserveModelCollectionsUseCase
typealias RemoveModelFromCollectionUseCase = Feature_collectionsRemoveModelFromCollectionUseCase
typealias RenameCollectionUseCase = Feature_collectionsRenameCollectionUseCase

// MARK: - Feature: Search

typealias AddExcludedTagUseCase = Feature_searchAddExcludedTagUseCase
typealias AddSearchHistoryUseCase = Feature_searchAddSearchHistoryUseCase
typealias ClearSearchHistoryUseCase = Feature_searchClearSearchHistoryUseCase
typealias GetDiscoveryModelsUseCase = Feature_searchGetDiscoveryModelsUseCase
typealias GetExcludedTagsUseCase = Feature_searchGetExcludedTagsUseCase
typealias GetHiddenModelIdsUseCase = Feature_searchGetHiddenModelIdsUseCase
typealias GetModelsUseCase = Feature_searchGetModelsUseCase
typealias GetRecommendationsUseCase = Feature_searchGetRecommendationsUseCase
typealias HideModelUseCase = Feature_searchHideModelUseCase
typealias ObserveSearchHistoryUseCase = Feature_searchObserveSearchHistoryUseCase
typealias RemoveExcludedTagUseCase = Feature_searchRemoveExcludedTagUseCase
typealias UnhideModelUseCase = Feature_searchUnhideModelUseCase

// MARK: - Feature: Gallery

typealias EnrichModelImagesUseCase = Feature_galleryEnrichModelImagesUseCase
typealias GetImagesUseCase = Feature_galleryGetImagesUseCase

// MARK: - Feature: Prompts

typealias AutoSavePromptUseCase = Feature_promptsAutoSavePromptUseCase
typealias DeleteSavedPromptUseCase = Feature_promptsDeleteSavedPromptUseCase
typealias ObserveSavedPromptsUseCase = Feature_promptsObserveSavedPromptsUseCase
typealias ObserveTemplatesUseCase = Feature_promptsObserveTemplatesUseCase
typealias SavePromptUseCase = Feature_promptsSavePromptUseCase
typealias SearchSavedPromptsUseCase = Feature_promptsSearchSavedPromptsUseCase
typealias ToggleTemplateUseCase = Feature_promptsToggleTemplateUseCase

// MARK: - Feature: Creator

typealias GetCreatorModelsUseCase = Feature_creatorGetCreatorModelsUseCase

// MARK: - Feature: ComfyUI — Workflow Templates

typealias ComfyUIGeneratedImage = Core_domainComfyUIGeneratedImage
typealias ComfyUIGenerationMeta = Core_domainComfyUIGenerationMeta
typealias WorkflowTemplate = Core_domainWorkflowTemplate
typealias WorkflowTemplateType = Core_domainWorkflowTemplateType
typealias TemplateVariable = Core_domainTemplateVariable
typealias TemplateVariableType = Core_domainTemplateVariableType
typealias GetWorkflowTemplatesUseCase = Feature_comfyuiGetWorkflowTemplatesUseCase
typealias SaveWorkflowTemplateUseCase = Feature_comfyuiSaveWorkflowTemplateUseCase
typealias DeleteWorkflowTemplateUseCase = Feature_comfyuiDeleteWorkflowTemplateUseCase
typealias ExportWorkflowTemplateUseCase = Feature_comfyuiExportWorkflowTemplateUseCase
typealias ImportWorkflowTemplateUseCase = Feature_comfyuiImportWorkflowTemplateUseCase
typealias ApplyWorkflowTemplateUseCase = Feature_comfyuiApplyWorkflowTemplateUseCase

// MARK: - Feature: Settings

typealias SettingsViewModel = Feature_settingsSettingsViewModel

// MARK: - ModelImage Swift Extensions
// Kotlin extension functions are not exported to Swift, so we mirror them here.

extension Core_domainModelImage {
    /// Returns a CDN URL resized to the given width.
    /// CivitAI CDN format: .../xG1nkqKTMzGDvpLrqFT7WA/{uuid}/width={size}/{filename}
    func thumbnailUrl(width: Int) -> String? {
        guard !url.isEmpty else { return nil }
        guard url.contains("image.civitai.com") else { return url }
        var parts = url.split(separator: "/", omittingEmptySubsequences: false).map(String.init)
        if let widthIdx = parts.firstIndex(where: { $0.hasPrefix("width=") }) {
            parts[widthIdx] = "width=\(width)"
        } else if parts.count > 5 {
            parts.insert("width=\(width)", at: 5)
        }
        return parts.joined(separator: "/")
    }
}
