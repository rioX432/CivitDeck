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
import com.riox432.civitdeck.domain.usecase.AddModelToCollectionUseCase
import com.riox432.civitdeck.domain.usecase.AddPersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.CancelDownloadUseCase
import com.riox432.civitdeck.domain.usecase.CreateCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeleteModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.EmbedOnBrowseUseCase
import com.riox432.civitdeck.domain.usecase.EnqueueDownloadUseCase
import com.riox432.civitdeck.domain.usecase.EnrichModelImagesUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.GetModelReviewsUseCase
import com.riox432.civitdeck.domain.usecase.GetRatingTotalsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelDownloadsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObservePersonalTagsUseCase
import com.riox432.civitdeck.domain.usecase.ObservePowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.RemoveModelFromCollectionUseCase
import com.riox432.civitdeck.domain.usecase.RemovePersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.SaveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.SubmitReviewUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
import com.riox432.civitdeck.domain.util.currentTimeMillis
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
    val isLoading: Boolean = true,
    val error: String? = null,
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
)

@Suppress("LongParameterList", "TooManyFunctions")
class ModelDetailViewModel(
    private val modelId: Long,
    private val getModelDetailUseCase: GetModelDetailUseCase,
    private val observeIsFavoriteUseCase: ObserveIsFavoriteUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val trackModelViewUseCase: TrackModelViewUseCase,
    private val observeNsfwFilterUseCase: ObserveNsfwFilterUseCase,
    private val enrichModelImagesUseCase: EnrichModelImagesUseCase,
    observeCollectionsUseCase: ObserveCollectionsUseCase,
    private val observeModelCollectionsUseCase: ObserveModelCollectionsUseCase,
    private val addModelToCollectionUseCase: AddModelToCollectionUseCase,
    private val removeModelFromCollectionUseCase: RemoveModelFromCollectionUseCase,
    private val createCollectionUseCase: CreateCollectionUseCase,
    private val observePowerUserModeUseCase: ObservePowerUserModeUseCase,
    private val observeModelNoteUseCase: ObserveModelNoteUseCase,
    private val saveModelNoteUseCase: SaveModelNoteUseCase,
    private val deleteModelNoteUseCase: DeleteModelNoteUseCase,
    private val observePersonalTagsUseCase: ObservePersonalTagsUseCase,
    private val addPersonalTagUseCase: AddPersonalTagUseCase,
    private val removePersonalTagUseCase: RemovePersonalTagUseCase,
    private val observeModelDownloadsUseCase: ObserveModelDownloadsUseCase,
    private val enqueueDownloadUseCase: EnqueueDownloadUseCase,
    private val cancelDownloadUseCase: CancelDownloadUseCase,
    private val getModelReviewsUseCase: GetModelReviewsUseCase,
    private val getRatingTotalsUseCase: GetRatingTotalsUseCase,
    private val submitReviewUseCase: SubmitReviewUseCase,
    private val embedOnBrowseUseCase: EmbedOnBrowseUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelDetailUiState())
    val uiState: StateFlow<ModelDetailUiState> = _uiState.asStateFlow()
    private val enrichedVersionIds = MutableStateFlow<Set<Long>>(emptySet())
    private var viewStartTimeMs: Long = 0L

    private val reviewDelegate = DetailReviewDelegate(
        modelId = modelId,
        scope = viewModelScope,
        uiState = _uiState,
        getModelReviewsUseCase = getModelReviewsUseCase,
        getRatingTotalsUseCase = getRatingTotalsUseCase,
        submitReviewUseCase = submitReviewUseCase,
    )

    private val notesTagsDelegate = DetailNotesTagsDelegate(
        modelId = modelId,
        scope = viewModelScope,
        saveModelNoteUseCase = saveModelNoteUseCase,
        deleteModelNoteUseCase = deleteModelNoteUseCase,
        addPersonalTagUseCase = addPersonalTagUseCase,
        removePersonalTagUseCase = removePersonalTagUseCase,
    )

    private val _downloadEnqueuedEvent = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val downloadEnqueuedEvent: SharedFlow<Long> = _downloadEnqueuedEvent

    val collections: StateFlow<List<ModelCollection>> =
        observeCollectionsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val modelCollectionIds: StateFlow<List<Long>> =
        observeModelCollectionsUseCase(modelId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    private val collectionDelegate = DetailCollectionDelegate(
        scope = viewModelScope,
        modelCollectionIds = modelCollectionIds,
        addModelToCollectionUseCase = addModelToCollectionUseCase,
        removeModelFromCollectionUseCase = removeModelFromCollectionUseCase,
        createCollectionUseCase = createCollectionUseCase,
    )

    private val downloadDelegate = DetailDownloadDelegate(
        modelId = modelId,
        scope = viewModelScope,
        enqueueDownloadUseCase = enqueueDownloadUseCase,
        cancelDownloadUseCase = cancelDownloadUseCase,
        trackModelViewUseCase = trackModelViewUseCase,
        downloadEnqueuedEvent = _downloadEnqueuedEvent,
    )

    init {
        loadModel()
        observeFavorite()
        startObservers()
        reviewDelegate.loadReviews()
    }

    fun onVersionSelected(index: Int) {
        _uiState.update { it.copy(selectedVersionIndex = index) }
        enrichCurrentVersion()
    }

    override fun onCleared() {
        super.onCleared()
        trackEndView()
    }

    fun onFavoriteToggle() {
        val model = _uiState.value.model ?: return
        launchCatching("Favorite toggle") {
            toggleFavoriteUseCase(model)
            trackModelViewUseCase.trackInteraction(modelId, InteractionType.FAVORITE)
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
            suspendRunCatching { getModelDetailUseCase(modelId) }
                .onSuccess { model ->
                    _uiState.update { it.copy(model = model, isLoading = false) }
                    enrichCurrentVersion()
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
            suspendRunCatching { enrichModelImagesUseCase(version.id, version.images) }
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
            observeIsFavoriteUseCase(modelId).collect { isFavorite ->
                _uiState.update { it.copy(isFavorite = isFavorite) }
            }
        }
    }

    private fun observeNsfwFilter() {
        viewModelScope.launch {
            observeNsfwFilterUseCase().collect { level ->
                _uiState.update { it.copy(nsfwFilterLevel = level) }
            }
        }
    }

    private fun observePowerUserMode() {
        viewModelScope.launch {
            observePowerUserModeUseCase().collect { enabled ->
                _uiState.update { it.copy(powerUserMode = enabled) }
            }
        }
    }

    private fun observeNote() {
        viewModelScope.launch {
            observeModelNoteUseCase(modelId).collect { note ->
                _uiState.update { it.copy(note = note) }
            }
        }
    }

    private fun observePersonalTags() {
        viewModelScope.launch {
            observePersonalTagsUseCase(modelId).collect { tags ->
                _uiState.update { it.copy(personalTags = tags) }
            }
        }
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            observeModelDownloadsUseCase(modelId).collect { downloads ->
                _uiState.update { it.copy(downloads = downloads) }
            }
        }
    }

    // endregion

    // region Private — Helpers

    /**
     * Common pattern: launch a coroutine, run [block] inside suspendRunCatching,
     * and log failures with [operationName].
     */
    private fun launchCatching(operationName: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            suspendRunCatching { block() }
                .onFailure { e -> Logger.w(TAG, "$operationName failed: ${e.message}") }
        }
    }

    /**
     * Fire-and-forget background embed of the model's first thumbnail.
     * Silent on failure — never blocks or surfaces errors to the user.
     */
    private fun triggerBackgroundEmbed(model: Model) {
        val url = model.modelVersions.firstOrNull()
            ?.images?.firstOrNull()?.url ?: return
        viewModelScope.launch {
            suspendRunCatching { embedOnBrowseUseCase(model.id, url) }
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
                        trackModelViewUseCase.endView(modelId, durationMs)
                    }
                } catch (e: Exception) {
                    Logger.w(TAG, "End view tracking failed: ${e.message}")
                }
            }
        }
    }

    private suspend fun trackModelView(model: Model) {
        trackModelViewUseCase(
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
