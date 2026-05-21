package com.riox432.civitdeck.feature.detail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.InteractionType
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelCollection
import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.model.ModelFile
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.model.ModelNote
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.PersonalTag
import com.riox432.civitdeck.domain.model.RatingTotals
import com.riox432.civitdeck.domain.model.ResourceReview
import com.riox432.civitdeck.domain.model.ReviewSortOrder
import com.riox432.civitdeck.domain.model.SystemStats
import com.riox432.civitdeck.domain.util.SystemStatsProvider
import com.riox432.civitdeck.domain.util.UiLoadingState
import com.riox432.civitdeck.domain.util.VramCompatibility
import com.riox432.civitdeck.domain.util.calculateVramCompatibility
import com.riox432.civitdeck.domain.util.currentTimeMillis
import com.riox432.civitdeck.domain.util.launchSafe
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class ModelDetailUiState(
    val model: Model? = null,
    val isFavorite: Boolean = false,
    override val isLoading: Boolean = true,
    override val error: String? = null,
    val selectedVersionIndex: Int = 0,
    val nsfwFilterLevel: NsfwFilterLevel = NsfwFilterLevel.Off,
    val powerUserMode: Boolean = false,
    val note: ModelNote? = null,
    val personalTags: List<PersonalTag> = emptyList(),
    val downloads: List<ModelDownload> = emptyList(),
    val reviews: List<ResourceReview> = emptyList(),
    val ratingTotals: RatingTotals? = null,
    val reviewSortOrder: ReviewSortOrder = ReviewSortOrder.Newest,
    val isReviewsLoading: Boolean = false,
    val reviewsError: String? = null,
    val isSubmittingReview: Boolean = false,
    val reviewSubmitSuccess: Boolean = false,
    val systemStats: SystemStats? = null,
    val fileVramCompatibility: Map<Long, VramCompatibility> = emptyMap(),
) : UiLoadingState

class ModelDetailViewModel(
    private val modelId: Long,
    private val modelUseCases: ModelUseCases,
    private val collectionUseCases: CollectionUseCases,
    private val notesTagsUseCases: NotesTagsUseCases,
    private val downloadUseCases: DownloadUseCases,
    private val reviewUseCases: ReviewUseCases,
    private val systemStatsProvider: SystemStatsProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelDetailUiState())
    val uiState: StateFlow<ModelDetailUiState> = _uiState.asStateFlow()
    private val enrichedVersionIds = MutableStateFlow<Set<Long>>(emptySet())
    private var viewStartTimeMs: Long = 0L

    private val reviewDelegate = DetailReviewDelegate(
        modelId = modelId,
        scope = viewModelScope,
        uiState = _uiState,
        getModelReviewsUseCase = reviewUseCases.getModelReviews,
        getRatingTotalsUseCase = reviewUseCases.getRatingTotals,
        submitReviewUseCase = reviewUseCases.submitReview,
    )

    private val notesTagsDelegate = DetailNotesTagsDelegate(
        modelId = modelId,
        scope = viewModelScope,
        saveModelNoteUseCase = notesTagsUseCases.saveModelNote,
        deleteModelNoteUseCase = notesTagsUseCases.deleteModelNote,
        addPersonalTagUseCase = notesTagsUseCases.addPersonalTag,
        removePersonalTagUseCase = notesTagsUseCases.removePersonalTag,
    )

    private val _downloadEnqueuedEvent = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val downloadEnqueuedEvent: SharedFlow<Long> = _downloadEnqueuedEvent

    val collections: StateFlow<List<ModelCollection>> =
        collectionUseCases.observeCollections()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val modelCollectionIds: StateFlow<List<Long>> =
        collectionUseCases.observeModelCollections(modelId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    private val collectionDelegate = DetailCollectionDelegate(
        scope = viewModelScope,
        modelCollectionIds = modelCollectionIds,
        addModelToCollectionUseCase = collectionUseCases.addModelToCollection,
        removeModelFromCollectionUseCase = collectionUseCases.removeModelFromCollection,
        createCollectionUseCase = collectionUseCases.createCollection,
    )

    private val downloadDelegate = DetailDownloadDelegate(
        modelId = modelId,
        scope = viewModelScope,
        enqueueDownloadUseCase = downloadUseCases.enqueueDownload,
        cancelDownloadUseCase = downloadUseCases.cancelDownload,
        trackModelViewUseCase = modelUseCases.trackModelView,
        downloadEnqueuedEvent = _downloadEnqueuedEvent,
    )

    init {
        loadModel()
        observeFavorite()
        startObservers()
        reviewDelegate.loadReviews()
        fetchSystemStats()
    }

    fun onVersionSelected(index: Int) {
        _uiState.update { it.copy(selectedVersionIndex = index) }
        enrichCurrentVersion()
        updateVramCompatibility()
    }

    override fun onCleared() {
        super.onCleared()
        trackEndView()
    }

    fun onFavoriteToggle() {
        val model = _uiState.value.model ?: return
        viewModelScope.launchSafe(TAG, "Favorite toggle") {
            modelUseCases.toggleFavorite(model)
            modelUseCases.trackModelView.trackInteraction(modelId, InteractionType.FAVORITE)
        }
        triggerBackgroundEmbed(model)
    }

    fun retry() {
        loadModel()
    }

    // region Notes & Tags (delegated)

    fun saveNote(text: String) = notesTagsDelegate.saveNote(text)
    fun addTag(tag: String) = notesTagsDelegate.addTag(tag)
    fun removeTag(tag: String) = notesTagsDelegate.removeTag(tag)

    // endregion

    // region Collections (delegated)

    fun toggleCollection(collectionId: Long) =
        collectionDelegate.toggleCollection(collectionId, _uiState.value.model)

    fun createCollectionAndAdd(name: String) =
        collectionDelegate.createCollectionAndAdd(name, _uiState.value.model)

    // endregion

    // region Downloads (delegated)

    fun downloadFile(file: ModelFile) {
        val state = _uiState.value
        val model = state.model ?: return
        val version = model.modelVersions.getOrNull(state.selectedVersionIndex)
        downloadDelegate.downloadFile(file, model, version)
    }

    fun cancelDownload(downloadId: Long) = downloadDelegate.cancelDownload(downloadId)

    // endregion

    // region Reviews

    fun onReviewSortChanged(order: ReviewSortOrder) = reviewDelegate.onReviewSortChanged(order)

    fun submitReview(
        modelVersionId: Long,
        rating: Int,
        recommended: Boolean,
        details: String?,
    ) = reviewDelegate.submitReview(modelVersionId, rating, recommended, details)

    fun dismissReviewSuccess() = reviewDelegate.dismissReviewSuccess()

    // endregion

    // region Private — Model loading & enrichment

    private fun loadModel() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            suspendRunCatching { modelUseCases.getModelDetail(modelId) }
                .onSuccess { model ->
                    _uiState.update { it.copy(model = model, isLoading = false) }
                    enrichCurrentVersion()
                    updateVramCompatibility()
                    trackModelView(model)
                    triggerBackgroundEmbed(model)
                    viewStartTimeMs = currentTimeMillis()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Unknown error")
                    }
                }
        }
    }

    private fun enrichCurrentVersion() {
        val state = _uiState.value
        val model = state.model ?: return
        val version = model.modelVersions.getOrNull(state.selectedVersionIndex) ?: return
        if (!markVersionForEnrichment(version.id)) return
        viewModelScope.launch {
            suspendRunCatching { modelUseCases.enrichModelImages(version.id, version.images) }
                .onSuccess { enriched -> applyEnrichedImages(version.id, enriched) }
                .onFailure { e ->
                    Logger.w(TAG, "Enrich version images failed: ${e.message}")
                    enrichedVersionIds.update { it - version.id }
                }
        }
    }

    /**
     * Marks a version as being enriched. Returns true if enrichment should proceed
     * (i.e., the version was not already enriched).
     */
    private fun markVersionForEnrichment(versionId: Long): Boolean {
        var shouldEnrich = false
        enrichedVersionIds.update { ids ->
            if (versionId in ids) {
                ids
            } else {
                shouldEnrich = true
                ids + versionId
            }
        }
        return shouldEnrich
    }

    private fun applyEnrichedImages(versionId: Long, enriched: List<ModelImage>) {
        _uiState.update { current ->
            val currentModel = current.model ?: return@update current
            val updatedVersions = currentModel.modelVersions.map { v ->
                if (v.id == versionId) v.copy(images = enriched) else v
            }
            current.copy(model = currentModel.copy(modelVersions = updatedVersions))
        }
    }

    // endregion

    // region Private — Observers

    private fun startObservers() {
        observeNsfwFilter()
        observePowerUserMode()
        observeNote()
        observePersonalTags()
        observeDownloads()
    }

    private fun observeFavorite() {
        viewModelScope.launch {
            modelUseCases.observeIsFavorite(modelId).collect { isFavorite ->
                _uiState.update { it.copy(isFavorite = isFavorite) }
            }
        }
    }

    private fun observeNsfwFilter() {
        viewModelScope.launch {
            modelUseCases.observeNsfwFilter().collect { level ->
                _uiState.update { it.copy(nsfwFilterLevel = level) }
            }
        }
    }

    private fun observePowerUserMode() {
        viewModelScope.launch {
            modelUseCases.observePowerUserMode().collect { enabled ->
                _uiState.update { it.copy(powerUserMode = enabled) }
            }
        }
    }

    private fun observeNote() {
        viewModelScope.launch {
            notesTagsUseCases.observeModelNote(modelId).collect { note ->
                _uiState.update { it.copy(note = note) }
            }
        }
    }

    private fun observePersonalTags() {
        viewModelScope.launch {
            notesTagsUseCases.observePersonalTags(modelId).collect { tags ->
                _uiState.update { it.copy(personalTags = tags) }
            }
        }
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            downloadUseCases.observeModelDownloads(modelId).collect { downloads ->
                _uiState.update { it.copy(downloads = downloads) }
            }
        }
    }

    // endregion

    // region Private — System Stats

    private fun fetchSystemStats() {
        viewModelScope.launch {
            val stats = systemStatsProvider.fetch() ?: return@launch
            _uiState.update { it.copy(systemStats = stats) }
            updateVramCompatibility()
        }
    }

    private fun updateVramCompatibility() {
        val state = _uiState.value
        val stats = state.systemStats ?: return
        val model = state.model ?: return
        val version = model.modelVersions.getOrNull(state.selectedVersionIndex) ?: return
        val compatMap = version.files.associate { file ->
            file.id to calculateVramCompatibility(file.sizeKB, stats.vramTotalMB)
        }
        _uiState.update { it.copy(fileVramCompatibility = compatMap) }
    }

    // endregion

    // region Private — Helpers

    /**
     * Fire-and-forget background embed of the model's first thumbnail.
     * Silent on failure — never blocks or surfaces errors to the user.
     */
    private fun triggerBackgroundEmbed(model: Model) {
        val url = model.modelVersions.firstOrNull()
            ?.images?.firstOrNull()?.url ?: return
        viewModelScope.launch {
            suspendRunCatching { modelUseCases.embedOnBrowse(model.id, url) }
                .onFailure { e -> Logger.d(TAG, "Background embed skipped: ${e.message}") }
        }
    }

    private fun trackEndView() {
        if (viewStartTimeMs <= 0L) return
        val durationMs = currentTimeMillis() - viewStartTimeMs
        viewStartTimeMs = 0L
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    withTimeoutOrNull(END_VIEW_TIMEOUT) {
                        modelUseCases.trackModelView.endView(modelId, durationMs)
                    }
                } catch (e: Exception) {
                    Logger.w(TAG, "End view tracking failed: ${e.message}")
                }
            }
        }
    }

    private suspend fun trackModelView(model: Model) {
        modelUseCases.trackModelView(
            modelId = model.id,
            modelName = model.name,
            modelType = model.type.name,
            creatorName = model.creator?.username,
            thumbnailUrl = model.modelVersions.firstOrNull()
                ?.images?.firstOrNull()?.url,
            tags = model.tags,
        )
    }

    // endregion
}

private const val TAG = "ModelDetailViewModel"
private const val STOP_TIMEOUT = 5_000L
private const val END_VIEW_TIMEOUT = 5_000L
